package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.service.PlayerService;
import com.tombtale.serviceplayer.util.LogUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;

    /**
     * GET /api/v1/players/me
     * <p>
     * Returns the current authenticated player's profile.
     * If the player doesn't exist yet, creates a new profile automatically.
     */
    @GetMapping("/me")
    public ResponseEntity<PlayerResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String zitadelUserId = jwt.getSubject();
        log.debug("Fetching profile for Zitadel user: {}", LogUtils.maskId(zitadelUserId));

        Player player = playerService.getOrCreatePlayer(zitadelUserId);

        return ResponseEntity.ok(playerMapper.toResponse(player));
    }

    /**
     * PATCH /api/v1/players/me
     * <p>
     * Updates the current authenticated player's profile (e.g., displayName,
     * profileIcon).
     */
    @PatchMapping("/me")
    public ResponseEntity<PlayerResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody com.tombtale.serviceplayer.dto.UpdateMyProfileRequest request) {
        String zitadelUserId = jwt.getSubject();
        log.debug("Updating profile for Zitadel user: {}", LogUtils.maskId(zitadelUserId));

        PlayerResponse updated = playerService.updateMyProfile(zitadelUserId, request);
        return ResponseEntity.ok(updated);
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
}
