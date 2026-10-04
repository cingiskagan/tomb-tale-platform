package com.tombtale.servicecommerce.security;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

/**
 * Names the principal behind the current write, and for now never can, so {@code createdBy} stays null.
 * ADR 0023 puts the caller's {@code publicId} in the access token, and this class will read it there.
 */
public class CommerceAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.empty();
    }
}
