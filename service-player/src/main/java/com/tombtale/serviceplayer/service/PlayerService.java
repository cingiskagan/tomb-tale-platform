package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

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

    /** Random characters in a default display name, as in "Player_a1b2c3d4". */
    private static final int DISPLAY_NAME_ID_PREFIX_LENGTH = 8;

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;

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
     * Returns the caller's profile, and creates it on their first call with the
     * {@code publicId} Keycloak minted for the account (ADR 0024).
     *
     * <p>{@code NOT_SUPPORTED} is not decoration. The class declares
     * {@code readOnly = true}, which would make the creation below a write in a
     * read-only transaction. Running outside one also lets {@code save} keep its
     * own transaction, so a collision can be caught and retried instead of
     * poisoning an outer one.
     *
     * @param publicId   the {@code public_id} claim of the caller's token
     * @param keycloakId the {@code sub} claim, kept on a new row
     * @return the existing or newly created profile
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlayerResponse getOrCreatePlayer(UUID publicId, String keycloakId) {
        Player player = playerRepository.findByPublicIdWithCharacters(publicId)
                .orElseGet(() -> createOrRecoverPlayer(publicId, keycloakId));

        return playerMapper.toResponse(player);
    }

    /**
     * Creates the player, or returns the row a concurrent first call committed first.
     *
     * @param publicId   the {@code public_id} claim of the caller's token
     * @param keycloakId the {@code sub} claim of the caller's token
     * @return the created or recovered player
     */
    private Player createOrRecoverPlayer(UUID publicId, String keycloakId) {
        try {
            return createNewPlayerWithCharacter(publicId, keycloakId);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent creation detected for player {}. Fetching existing record.", publicId);
            return playerRepository.findByPublicIdWithCharacters(publicId)
                    .orElseThrow(() -> new IllegalStateException("Failed to find player after creation collision"));
        }
    }

    /**
     * Creates the player with a default character. The player, the character
     * and the {@code player.created} outbox row commit together or not at all.
     *
     * @param publicId   the {@code public_id} claim of the caller's token
     * @param keycloakId the {@code sub} claim of the caller's token
     * @return the newly created player
     */
    private Player createNewPlayerWithCharacter(UUID publicId, String keycloakId) {
        log.info("Creating player {}", publicId);
        String defaultDisplayName = "Player_" + UUID.randomUUID().toString().substring(0, DISPLAY_NAME_ID_PREFIX_LENGTH);

        Player newPlayer = Player.builder()
                .publicId(publicId)
                .keycloakId(keycloakId)
                .displayName(defaultDisplayName)
                .build();

        GameCharacter initialCharacter = GameCharacter.builder()
                .name(defaultDisplayName)
                .player(newPlayer)
                .build();

        newPlayer.addCharacter(initialCharacter);

        return transactionTemplate.execute(status -> {
            Player saved = playerRepository.save(newPlayer);
            outboxService.append(
                    PlayerCreatedPayload.EVENT_TYPE,
                    PlayerCreatedPayload.EVENT_VERSION,
                    saved.getPublicId(),
                    new PlayerCreatedPayload(
                            saved.getPublicId(),
                            saved.getDisplayName(),
                            initialCharacter.getPublicId()));
            return saved;
        });
    }

    /**
     * Updates the player's profile information.
     *
     * @param publicId the {@code public_id} claim of the caller's token
     * @param request  the profile update request
     * @return the updated player response
     */
    @Transactional
    public PlayerResponse updateMyProfile(UUID publicId, UpdateMyProfileRequest request) {
        Player player = playerRepository.findByPublicIdWithCharacters(publicId)
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
