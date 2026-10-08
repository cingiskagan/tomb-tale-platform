package com.tombtale.commons.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class PublicIdClaimValidatorTest {

    private final PublicIdClaimValidator validator = new PublicIdClaimValidator();

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token").header("alg", "none").subject("user");
    }

    @Test
    void acceptsAUuid() {
        assertThat(validator.validate(token().claim(PublicIdClaim.CLAIM, "6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f3b64").build())
                .hasErrors()).isFalse();
    }

    /** A missing claim is the controller's decision, not the filter's. */
    @Test
    void acceptsATokenWithoutTheClaim() {
        assertThat(validator.validate(token().build()).hasErrors()).isFalse();
    }

    @Test
    void rejectsAValueThatIsNotAUuid() {
        OAuth2TokenValidatorResult result = validator.validate(token().claim(PublicIdClaim.CLAIM, "not-a-uuid").build());

        assertThat(result.getErrors())
                .singleElement()
                .satisfies(error -> assertThat(error.getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_TOKEN));
    }
}
