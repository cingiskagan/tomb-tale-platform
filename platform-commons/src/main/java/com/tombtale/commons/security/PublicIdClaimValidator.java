package com.tombtale.commons.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects a token whose {@code public_id} is not a UUID, so the request fails with 401 at the
 * filter, before any controller or write. A token without the claim passes here.
 */
public class PublicIdClaimValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error MALFORMED = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "The public_id claim is not a UUID", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            PublicIdClaim.read(jwt);
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException e) {
            return OAuth2TokenValidatorResult.failure(MALFORMED);
        }
    }
}
