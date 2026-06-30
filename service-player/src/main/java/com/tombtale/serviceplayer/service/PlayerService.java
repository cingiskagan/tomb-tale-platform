package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

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
     * @param zitadelUserId the subject claim from the JWT
     * @return the existing or newly created player
     */
    @Transactional
    public Player getOrCreatePlayer(String zitadelUserId) {
        return playerRepository.findByZitadelUserId(zitadelUserId)
                .map(this::backfillCharacterIfMissing)
                .orElseGet(() -> createNewPlayerWithCharacter(zitadelUserId));
    }

    /**
     * Creates a default character for players that existed before the
     * character system was introduced. This is a self-healing migration:
     * each player gets backfilled on their next login.
     *
     * @param player the existing player to check
     * @return the player, with a character guaranteed
     */
    private Player backfillCharacterIfMissing(Player player) {
        if (!player.getCharacters().isEmpty()) {
            return player;
        }

        log.info("Backfilling default character for existing player: {}",
                player.getPublicId());

        GameCharacter backfilledCharacter = GameCharacter.builder()
                .name(player.getDisplayName())
                .player(player)
                .build();

        player.getCharacters().add(backfilledCharacter);
        return playerRepository.save(player);
    }

    /**
     * Creates a brand-new player with a default character (JIT provisioning).
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the newly created player
     */
    private Player createNewPlayerWithCharacter(String zitadelUserId) {
        log.info("Creating new player profile for Zitadel user");
        String defaultDisplayName = "Player_" + zitadelUserId.substring(
                0, Math.min(zitadelUserId.length(), DISPLAY_NAME_ID_PREFIX_LENGTH));

        Player newPlayer = Player.builder()
                .zitadelUserId(zitadelUserId)
                .displayName(defaultDisplayName)
                .build();

        GameCharacter initialCharacter = GameCharacter.builder()
                .name(defaultDisplayName)
                .player(newPlayer)
                .build();

        newPlayer.getCharacters().add(initialCharacter);

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
        Player player = playerRepository.findByZitadelUserId(zitadelUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        if (!player.getDisplayName().equalsIgnoreCase(request.getDisplayName()) &&
                playerRepository.existsByDisplayName(request.getDisplayName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Display name is already taken");
        }

        player.setDisplayName(request.getDisplayName());

        if (request.getProfileIcon() != null && !request.getProfileIcon().isBlank()) {
            player.setProfileIcon(request.getProfileIcon());
        }

        Player saved = playerRepository.save(player);
        return playerMapper.toResponse(saved);
    }
}
