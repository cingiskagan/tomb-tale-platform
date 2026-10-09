package com.tombtale.commons.security;

import com.tombtale.commons.audit.SystemActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for {@link PlatformAuditorAware}, the class that decides who wrote a row. */
class PlatformAuditorAwareTest {

    private final PlatformAuditorAware auditorAware = new PlatformAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("the token's public_id claim is the author")
    void readsThePublicIdFromTheToken() {
        UUID publicId = UUID.randomUUID();
        authenticateWith(jwtToken(Jwt.withTokenValue("token").header("alg", "none")
                .subject("keycloak-user").claim(PublicIdClaim.CLAIM, publicId.toString()).build()));

        assertThat(auditorAware.getCurrentAuditor()).contains(publicId);
    }

    @Test
    @DisplayName("a system actor is the author of the work it runs")
    void readsTheSystemActor() {
        SystemActor actor = SystemActor.COMMERCE_PLAYER_EVENT_CONSUMER;

        assertThat(actor.run(auditorAware::getCurrentAuditor)).contains(actor.actorId());
    }

    @Test
    @DisplayName("a token without the claim is refused")
    void refusesATokenWithoutPublicId() {
        authenticateWith(jwtToken(Jwt.withTokenValue("token").header("alg", "none")
                .subject("keycloak-user").build()));

        assertThatThrownBy(auditorAware::getCurrentAuditor).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("nothing authenticated is refused")
    void refusesWithoutAuthentication() {
        assertThatThrownBy(auditorAware::getCurrentAuditor).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("an anonymous caller is refused")
    void refusesAnonymous() {
        authenticateWith(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(auditorAware::getCurrentAuditor).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("a principal that is neither a token nor a system actor is refused")
    void refusesAnyOtherPrincipal() {
        authenticateWith(new TestingAuthenticationToken("someone", "credentials", "player"));

        assertThatThrownBy(auditorAware::getCurrentAuditor).isInstanceOf(AccessDeniedException.class);
    }

    private static JwtAuthenticationToken jwtToken(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("player"));
    }

    private static void authenticateWith(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
