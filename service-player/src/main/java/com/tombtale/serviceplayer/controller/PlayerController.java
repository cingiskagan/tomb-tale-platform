package com.tombtale.serviceplayer.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * REST controller for player profile operations.
 * <p>
 * All endpoints require a valid Keycloak JWT. The caller is the player whose
 * {@code publicId} the token's {@code public_id} claim carries (ADR 0024).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    /**
     * POST /api/v1/players/me
     * <p>
     * Returns the current authenticated player's profile. The first call
     * creates it, with the {@code publicId} Keycloak minted for the account,
     * which is why this is a POST: a GET must never write.
     *
     * @param jwt the injected JWT token from the authenticated request
     * @return the player profile DTO
     */
    @PostMapping("/me")
    @PreAuthorize(RoleConstants.IS_AUTHENTICATED)
    public ResponseEntity<PlayerResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(playerService.getOrCreatePlayer(callerPublicId(jwt), jwt.getSubject()));
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
        PlayerResponse updated = playerService.updateMyProfile(callerPublicId(jwt), request);
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

    /** The caller's {@code publicId}. A token without the claim names no player, so it gets 401. */
    private static UUID callerPublicId(Jwt jwt) {
        return PublicIdClaim.read(jwt).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The token carries no public_id claim"));
    }
}
