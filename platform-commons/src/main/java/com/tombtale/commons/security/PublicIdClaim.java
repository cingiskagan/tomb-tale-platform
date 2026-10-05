package com.tombtale.commons.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Where an access token carries the player's {@code publicId} (ADR 0023).
 * service-player writes it as Zitadel user metadata, and Zitadel copies
 * every key into {@link #CLAIM}, base64-encoded.
 */
public final class PublicIdClaim {

    /**
     * The claim, present only when the client requests the scope of the same name.
     */
    public static final String CLAIM = "urn:zitadel:iam:user:metadata";

    /** The metadata key that holds the {@code publicId}. */
    public static final String KEY = "publicId";

    private PublicIdClaim() {
        // Constants holder.
    }

    /**
     * The player's {@code publicId} from an access token, or empty when the
     * token does not carry it.
     *
     * @param jwt the access token
     * @return the {@code publicId}, empty when the claim or the key is missing
     * @throws IllegalArgumentException if the value is not a base64-encoded UUID
     */
    public static Optional<UUID> read(Jwt jwt) {

        Object metadataObj = jwt.getClaims().get(CLAIM);

        if (metadataObj instanceof Map<?, ?> metadataMap && metadataMap.get(KEY) instanceof String base64PublicId) {
            try {
                String convertedId = new String(Base64.getDecoder().decode(base64PublicId), StandardCharsets.UTF_8);
                return Optional.of(UUID.fromString(convertedId));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("publicId claim is not a base64 UUID: " + e.getMessage(), e);
            }
        }

        return Optional.empty();
    }
}
