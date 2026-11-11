package com.tombtale.serviceinventory.security;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Who calls for a character's inventory: {@code platform_admin} may act on any character, and anyone else only
 * on a character they own (ADR 0031).
 *
 * @param playerId the token's {@code public_id}, or null when it has none
 * @param admin    whether the token carries {@code platform_admin}
 */
public record InventoryCaller(UUID playerId, boolean admin) {

    /**
     * @param token the caller's validated token
     * @return the caller it names
     */
    public static InventoryCaller from(JwtAuthenticationToken token) {
        boolean admin = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(RoleConstants.PLATFORM_ADMIN::equals);
        return new InventoryCaller(PublicIdClaim.read(token.getToken()).orElse(null), admin);
    }

    /**
     * @param character the character the caller asks for
     * @throws AccessDeniedException if the caller is no admin and doesn't own it, which answers 403
     */
    public void checkMayActOn(CharacterInventory character) {
        if (!admin && !character.getPlayerId().equals(playerId)) {
            throw new AccessDeniedException("The character belongs to another player");
        }
    }
}
