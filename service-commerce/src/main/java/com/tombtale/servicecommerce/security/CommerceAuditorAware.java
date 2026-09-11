package com.tombtale.servicecommerce.security;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

/**
 * Names the principal behind the current write — and, for now, never can.
 *
 * <p>A token reaching this service carries a Zitadel subject. The platform
 * identifies a player by the {@code publicId} that service-player issues, and
 * only service-player can turn one into the other. Commerce has no copy of
 * that mapping and no lookup to ask for it, so {@code createdBy} and
 * {@code updatedBy} stay null on every row it writes.
 *
 * <p>That gap is recorded in ADR 0013 and it closes in two steps: a local
 * replica of the players commerce has seen, then a call to service-player for
 * the ones it has not. After that a write nobody can be credited with is
 * rejected instead of stored, and this class resolves a real id.
 */
public class CommerceAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.empty();
    }
}
