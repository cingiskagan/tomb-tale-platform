package com.tombtale.serviceplayer.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.service.PlayerService;
import com.tombtale.serviceplayer.util.LogUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * GET /api/v1/players/me
     * <p>
     * Returns the current authenticated player's profile.
     * If the player doesn't exist yet, creates a new profile automatically.
     *
     * @param jwt the injected JWT token from the authenticated request
     * @return the player profile DTO
     */
    @GetMapping("/me")
    @PreAuthorize(RoleConstants.IS_AUTHENTICATED)
    public ResponseEntity<PlayerResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String zitadelUserId = jwt.getSubject();
        log.debug("Fetching profile for Zitadel user: {}", LogUtils.maskId(zitadelUserId));

        return ResponseEntity.ok(playerService.getOrCreatePlayer(zitadelUserId));
    }

    /**
     * PATCH /api/v1/players/me
     * <p>
     * Updates the current authenticated player's profile (e.g., displayName).
     *
     * @param jwt     the injected JWT token from the authenticated request
     * @param request the profile update request payload
     * @return the updated player profile DTO
     */
    @PatchMapping("/me")
    @PreAuthorize(RoleConstants.IS_AUTHENTICATED)
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
     * Supports dynamic filtering by display name.
     *
     * @param filter   optional query parameters for filtering
     * @param pageable pagination and sorting (e.g. ?page=0&size=20&sort=displayName,desc)
     * @return one page of players in the platform's envelope
     */
    @GetMapping
    @PreAuthorize(RoleConstants.IS_ADMIN_OR_GAME_MASTER)
    public ResponseEntity<PagedResponse<PlayerResponse>> listPlayers(
            @ModelAttribute PlayerFilterRequest filter,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(playerService.listPlayers(filter, pageable)));
    }
}
