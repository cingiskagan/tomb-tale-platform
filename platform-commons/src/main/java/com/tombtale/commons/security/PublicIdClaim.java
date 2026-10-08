package com.tombtale.commons.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

/**
 * Where an access token carries the player's {@code publicId}: Keycloak's
 * public-id mapper puts a UUID of the user's own in {@link #CLAIM} (ADR 0024).
 */
public final class PublicIdClaim {

    /** The claim, a UUID in its string form. */
    public static final String CLAIM = "public_id";

    private PublicIdClaim() {
        // Constants holder.
    }

    /**
     * The player's {@code publicId} from an access token, or empty when the
     * token does not carry it.
     *
     * @param jwt the access token
     * @return the {@code publicId}, empty when the claim is missing
     * @throws IllegalArgumentException if the value is not a UUID
     */
    public static Optional<UUID> read(Jwt jwt) {
        String value = jwt.getClaimAsString(CLAIM);

        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("publicId claim is not a UUID: " + e.getMessage(), e);
        }
    }
}
