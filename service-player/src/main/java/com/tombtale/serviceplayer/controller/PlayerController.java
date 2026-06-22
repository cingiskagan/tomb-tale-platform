package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdatePlayerStatsRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for player profile operations.
 * <p>
 * All endpoints require a valid Zitadel JWT.
 * The authenticated user is identified via the "sub" claim in the token.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    /**
     * Number of leading characters from the Zitadel user ID
     * used to generate a default display name (e.g. "Player_a1b2c3d4").
     */
    private static final int DISPLAY_NAME_ID_PREFIX_LENGTH = 8;

    /**
     * Minimum length required to safely mask an ID while retaining privacy.
     */
    private static final int MIN_MASKABLE_ID_LENGTH = 8;

    /**
     * Number of visible characters left unmasked at the start and end of an ID.
     */
    private static final int VISIBLE_ID_CHARS = 4;

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    /**
     * GET /api/v1/players/me
     * <p>
     * Returns the current authenticated player's profile.
     * If the player doesn't exist yet, creates a new profile automatically.
     */
    @GetMapping("/me")
    public ResponseEntity<Player> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String zitadelUserId = jwt.getSubject();
        log.debug("Fetching profile for Zitadel user: {}", maskId(zitadelUserId));

        Player player = playerRepository.findByZitadelUserId(zitadelUserId)
                .orElseGet(() -> {
                    log.info("Creating new player profile for Zitadel user: {}", maskId(zitadelUserId));
                    Player newPlayer = Player.builder()
                            .zitadelUserId(zitadelUserId)
                            .displayName("Player_" + zitadelUserId.substring(
                                    0, Math.min(zitadelUserId.length(), DISPLAY_NAME_ID_PREFIX_LENGTH)))
                            .build();
                    return playerRepository.save(newPlayer);
                });

        return ResponseEntity.ok(player);
    }

    /**
     * Masks an external identifier for safe logging (e.g., "1234abcd5678" ->
     * "1234***5678").
     * Keeps first 4 and last 4 characters visible for correlation.
     */
    private String maskId(String id) {
        if (id == null || id.length() <= MIN_MASKABLE_ID_LENGTH) {
            return "***"; // Too short to safely mask while retaining privacy
        }
        return id.substring(0, VISIBLE_ID_CHARS) + "***" +
                id.substring(id.length() - VISIBLE_ID_CHARS);
    }

    /**
     * GET /api/v1/players
     * <p>
     * Returns a paginated, filtered list of all players.
     * Supports dynamic filtering by display name and level range.
     *
     * @param filter   optional query parameters for filtering
     * @param pageable pagination and sorting (e.g. ?page=0&size=20&sort=level,desc)
     * @return a page of player response DTOs
     */
    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> listPlayers(
            @ModelAttribute PlayerFilterRequest filter,
            Pageable pageable) {
        Page<PlayerResponse> page = playerService.listPlayers(filter, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * PATCH /api/v1/players/{id}/stats
     * <p>
     * Updates the core progression stats of a player.
     * Restricted to admin/game master roles.
     *
     * @param id      internal player ID
     * @param request the new stats
     * @return the updated player
     */
    @PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")
    @PatchMapping("/{id}/stats")
    public ResponseEntity<PlayerResponse> updatePlayerStats(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlayerStatsRequest request) {
        log.info("Admin updating stats for player ID: {}, new level: {}, new XP: {}",
                id, request.getLevel(), request.getExperiencePoints());
        PlayerResponse updated = playerService.updatePlayerStats(id, request);
        return ResponseEntity.ok(updated);
    }
}
