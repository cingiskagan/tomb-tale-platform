package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.model.Player;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final PlayerRepository playerRepository;

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
                                    0, DISPLAY_NAME_ID_PREFIX_LENGTH))
                            .build();
                    return playerRepository.save(newPlayer);
                });

        return ResponseEntity.ok(player);
    }

    /**
     * Masks an external identifier for safe logging (e.g., "1234abcd5678" -> "1234***5678").
     * Keeps first 4 and last 4 characters visible for correlation.
     */
    private String maskId(String id) {
        if (id == null || id.length() <= 8) {
            return "***"; // Too short to safely mask while retaining privacy
        }
        return id.substring(0, 4) + "***" + id.substring(id.length() - 4);
    }
}
