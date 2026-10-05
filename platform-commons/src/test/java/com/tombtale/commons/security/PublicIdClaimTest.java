package com.tombtale.commons.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Pins the four shapes of the metadata claim: a valid {@code publicId}, no
 * claim, other keys only, and a value that is not a base64 UUID.
 */
class PublicIdClaimTest {

    private static final String TOKEN_VALUE = "token";
    private static final String ALG_KEY = "alg";
    private static final String ALG_VALUE = "none";

    private Jwt buildToken(String name, Object value) {
        return Jwt.withTokenValue(TOKEN_VALUE)
            .header(ALG_KEY, ALG_VALUE)
            .subject("user").claim(name, value)
            .build();
    }

    @Test
    void validClaimReturnsValue() {
        Jwt jwt = buildToken(PublicIdClaim.CLAIM, Map.of(PublicIdClaim.KEY, "NmYxYzJiOWUtNGQzYS00ZThiLTlhNzEtMmM1ZDhlMGYzYjY0"));

        Optional<UUID> publicId = PublicIdClaim.read(jwt);

        assertThat(publicId).contains(UUID.fromString("6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f3b64"));
    }

    @Test
    void missingClaimReturnsEmpty() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
            .header(ALG_KEY, ALG_VALUE)
            .subject("user")
            .build();

        Optional<UUID> publicId = PublicIdClaim.read(jwt);

        assertThat(publicId).isEmpty();
    }

    @Test
    void missingPublicIdReturnsEmpty() {
        Jwt jwt = buildToken(PublicIdClaim.CLAIM, Map.of("otherKey", "dmFsdWU="));

        Optional<UUID> publicId = PublicIdClaim.read(jwt);

        assertThat(publicId).isEmpty();
    }

    @Test
    void invalidUuidThrows() {
        Jwt jwt = buildToken(PublicIdClaim.CLAIM, Map.of(PublicIdClaim.KEY, "bm90LWEtdXVpZA=="));

        assertThatThrownBy(() -> PublicIdClaim.read(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("publicId claim is not a base64 UUID:");
    }
}
