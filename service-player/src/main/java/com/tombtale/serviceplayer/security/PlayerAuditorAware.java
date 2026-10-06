package com.tombtale.serviceplayer.security;

import com.tombtale.commons.security.PublicIdClaim;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Names the principal behind the current write, for {@code createdBy} and
 * {@code updatedBy}: the {@code publicId} the caller's token carries (ADR 0024).
 *
 * <p>Returns empty, leaving the column null, when nothing is authenticated,
 * the principal is not a JWT, or the token has no {@code public_id} claim.
 *
 * <p>Declared as a bean in {@code JpaConfig} rather than annotated
 * {@code @Component}: a {@code @DataJpaTest} slice loads repositories and
 * entities but no components, and auditing has to resolve its auditor in that
 * slice too.
 */
public class PlayerAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        return PublicIdClaim.read(jwt);
    }
}
