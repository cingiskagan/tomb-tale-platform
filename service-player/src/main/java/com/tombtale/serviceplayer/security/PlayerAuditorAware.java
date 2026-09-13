package com.tombtale.serviceplayer.security;

import com.tombtale.serviceplayer.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Names the principal behind the current write, for {@code createdBy} and
 * {@code updatedBy}.
 *
 * <p>The token carries a Zitadel subject, which is not an identifier this
 * platform stores anywhere but on {@code Player}. service-player owns that
 * table, so it can trade the subject for the player's {@code publicId}
 * itself. No other service can.
 *
 * <p>Returns empty, leaving the column null, in three cases: nothing is
 * authenticated, the principal is not a JWT, or the subject has no player
 * profile yet. The last one is the interesting one — it is every write in the
 * transaction that creates a player, because the row naming the author is the
 * row being written.
 *
 * <p>Declared as a bean in {@code JpaConfig} rather than annotated
 * {@code @Component}: a {@code @DataJpaTest} slice loads repositories and
 * entities but no components, and auditing has to resolve its auditor in that
 * slice too.
 */
@RequiredArgsConstructor
public class PlayerAuditorAware implements AuditorAware<UUID> {

    private final PlayerRepository playerRepository;

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        return playerRepository.findPublicIdByZitadelUserId(jwt.getSubject());
    }
}
