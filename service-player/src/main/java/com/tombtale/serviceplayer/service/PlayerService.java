package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.client.ZitadelClient;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.util.LogUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Business-logic layer for player read operations.
 *
 * <p>
 * Read-only methods default to {@code readOnly = true} for
 * Hibernate flush-mode optimisation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    /**
     * Number of leading characters from the Zitadel user ID
     * used to generate a default display name (e.g. "Player_a1b2c3d4").
     */
    private static final int DISPLAY_NAME_ID_PREFIX_LENGTH = 8;

    /**
     * The alarm for {@code GET /players/me} having to create a row.
     *
     * <p>Its own category, not this class's logger, so it can be silenced
     * without silencing anything else. The test profile turns it off: there is
     * no Zitadel there, so the fallback is the only path tests can take and an
     * alarm on every run would be noise. See ADR 0018.
     */
    private static final Logger PROVISIONING_FALLBACK =
            LoggerFactory.getLogger("com.tombtale.provisioning.fallback");

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final ZitadelClient zitadelClient;

    /**
     * Returns a paginated, filtered list of players.
     *
     * <p>
     * Delegates dynamic predicate construction to the QueryDSL
     * repository fragment.
     *
     * @param filter   optional filter criteria (all fields nullable)
     * @param pageable pagination and sorting parameters
     * @return a page of matching player DTOs
     */
    public Page<PlayerResponse> listPlayers(
            PlayerFilterRequest filter,
            Pageable pageable) {
        return playerRepository.findByFilter(filter, pageable)
                .map(playerMapper::toResponse);
    }

    /**
     * Retrieves a player profile by their Zitadel user ID, or creates a new one
     * if it does not exist yet (JIT Provisioning).
     *
     * <p>The entity never leaves this method. Mapping happens here so the
     * controller deals only in DTOs.
     *
     * <p>{@code NOT_SUPPORTED} is not decoration. The class declares
     * {@code readOnly = true}, which would make the creation below a write in a
     * read-only transaction. Running outside one also lets {@code save} keep its
     * own transaction, so a collision can be caught and retried instead of
     * poisoning an outer one.
     *
     * <p>Reading never writes. A player is always created together with a
     * default character, in one transaction, so an existing player is returned
     * exactly as it was stored.
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the existing or newly created profile
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlayerResponse getOrCreatePlayer(String zitadelUserId) {
        Player player = playerRepository.findByZitadelUserIdWithCharacters(zitadelUserId)
                .orElseGet(() -> {
                    PROVISIONING_FALLBACK.error(
                            "No player row for Zitadel user {} when reading their profile. The "
                                    + "provisioning event never arrived — check the Zitadel target and "
                                    + "execution. Creating the row now so the request succeeds.",
                            LogUtils.maskId(zitadelUserId));
                    return createOrRecoverPlayer(zitadelUserId);
                });

        return playerMapper.toResponse(player);
    }

    /**
     * Gives a newly self-registered user everything they need to play: the
     * player row, and the {@code player} role that lets them get a token at all.
     *
     * <p>This is the path the Zitadel event drives, so it is silent: creating a
     * row here is the design working. {@link #getOrCreatePlayer} raises an alarm
     * for the same write because reaching it there means this never ran.
     *
     * <p>The grant is attempted every time, not only when the row is new. The
     * two live in different systems and nothing makes them atomic, so a user
     * with a row and no role is a state this can be asked to repair.
     *
     * <p>Safe to call twice. Zitadel retries a target it could not reach, a
     * duplicate row lands on {@code uq_players_zitadel_user_id} rather than on a
     * second one, and a duplicate grant comes back as a conflict the client
     * treats as success.
     *
     * @param zitadelUserId the subject of the newly registered Zitadel user
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void provisionPlayer(String zitadelUserId) {
        if (playerRepository.findByZitadelUserIdWithCharacters(zitadelUserId).isPresent()) {
            log.debug("Player already exists for Zitadel user: {}", LogUtils.maskId(zitadelUserId));
        } else {
            createOrRecoverPlayer(zitadelUserId);
        }

        zitadelClient.grantPlayerRole(zitadelUserId);
    }

    /**
     * Creates the player, or returns the row a concurrent caller committed first.
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the created or recovered player
     */
    private Player createOrRecoverPlayer(String zitadelUserId) {
        try {
            return createNewPlayerWithCharacter(zitadelUserId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent creation detected for zitadel user: {}. Fetching existing record.",
                    LogUtils.maskId(zitadelUserId));
            return playerRepository.findByZitadelUserIdWithCharacters(zitadelUserId)
                    .orElseThrow(() -> new IllegalStateException("Failed to find player after creation collision"));
        }
    }

    /**
     * Creates a brand-new player with a default character (JIT provisioning).
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the newly created player
     */
    private Player createNewPlayerWithCharacter(String zitadelUserId) {
        log.info("Creating new player profile for Zitadel user");
        String defaultDisplayName = "Player_" + java.util.UUID.randomUUID().toString().substring(0, DISPLAY_NAME_ID_PREFIX_LENGTH);

        Player newPlayer = Player.builder()
                .zitadelUserId(zitadelUserId)
                .displayName(defaultDisplayName)
                .build();

        GameCharacter initialCharacter = GameCharacter.builder()
                .name(defaultDisplayName)
                .player(newPlayer)
                .build();

        newPlayer.addCharacter(initialCharacter);

        return playerRepository.save(newPlayer);
    }

    /**
     * Updates the player's profile information.
     *
     * @param zitadelUserId the subject claim from the JWT
     * @param request       the profile update request
     * @return the updated player response
     */
    @Transactional
    public PlayerResponse updateMyProfile(String zitadelUserId, UpdateMyProfileRequest request) {
        Player player = playerRepository.findByZitadelUserIdWithCharacters(zitadelUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        if (!player.getDisplayName().equalsIgnoreCase(request.getDisplayName()) &&
                playerRepository.existsByDisplayNameIgnoreCase(request.getDisplayName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Display name is already taken");
        }

        player.setDisplayName(request.getDisplayName());

        Player saved = playerRepository.save(player);
        return playerMapper.toResponse(saved);
    }
}
