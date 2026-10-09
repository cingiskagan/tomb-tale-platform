package com.tombtale.commons.security;

import com.tombtale.commons.audit.SystemActor;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Names the author of a write: the token's {@code publicId}, or a SystemActor.
 * Without one, it throws {@code AccessDeniedException}, a 403 (ADR 0013).
 */
public class PlatformAuditorAware implements AuditorAware<UUID> {

    private static final String NO_AUTHOR = "No author for this write";

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(NO_AUTHOR);
        }

        if (authentication.getPrincipal() instanceof SystemActor systemActor) {
            return Optional.of(systemActor.actorId());
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Optional<UUID> publicId = PublicIdClaim.read(jwt);
            if (publicId.isPresent()) {
                return publicId;
            }
        }

        throw new AccessDeniedException(NO_AUTHOR);
    }
}
