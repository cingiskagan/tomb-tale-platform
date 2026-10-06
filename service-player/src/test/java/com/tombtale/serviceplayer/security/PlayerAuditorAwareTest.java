package com.tombtale.serviceplayer.security;

import com.tombtale.commons.security.PublicIdClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PlayerAuditorAware}, the class that decides whether a
 * write can be attributed to anyone.
 */
class PlayerAuditorAwareTest {

    private final PlayerAuditorAware auditorAware = new PlayerAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("nothing authenticated leaves the column null")
    void returnsEmptyWithoutAuthentication() {
        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("an anonymous caller is not an auditor")
    void returnsEmptyForAnonymous() {
        authenticateWith(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("a principal that is not a JWT carries no publicId")
    void returnsEmptyForNonJwtPrincipal() {
        authenticateWith(new TestingAuthenticationToken("someone", "credentials", "player"));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("the token's public_id claim is the auditor")
    void readsThePublicIdFromTheToken() {
        UUID publicId = UUID.randomUUID();
        authenticateWith(jwtToken(Jwt.withTokenValue("token").header("alg", "none")
                .subject("keycloak-user").claim(PublicIdClaim.CLAIM, publicId.toString()).build()));

        assertThat(auditorAware.getCurrentAuditor()).contains(publicId);
    }

    @Test
    @DisplayName("a token without the claim leaves the column null")
    void returnsEmptyWhenTheTokenHasNoPublicId() {
        authenticateWith(jwtToken(Jwt.withTokenValue("token").header("alg", "none")
                .subject("keycloak-user").build()));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    private static JwtAuthenticationToken jwtToken(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("player"));
    }

    private static void authenticateWith(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
