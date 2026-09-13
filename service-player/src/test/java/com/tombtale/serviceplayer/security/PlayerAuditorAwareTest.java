package com.tombtale.serviceplayer.security;

import com.tombtale.serviceplayer.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlayerAuditorAware}, the class that decides whether a
 * write can be attributed to anyone.
 */
@ExtendWith(MockitoExtension.class)
class PlayerAuditorAwareTest {

    private static final String SUBJECT = "zitadel-sub-1";

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerAuditorAware auditorAware;

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
    @DisplayName("a principal that is not a JWT carries no subject to resolve")
    void returnsEmptyForNonJwtPrincipal() {
        authenticateWith(new TestingAuthenticationToken("someone", "credentials", "player"));

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    @DisplayName("a known subject resolves to that player's publicId")
    void resolvesTheSubjectToAPublicId() {
        UUID publicId = UUID.randomUUID();
        when(playerRepository.findPublicIdByZitadelUserId(SUBJECT)).thenReturn(Optional.of(publicId));
        authenticateWith(jwtToken());

        assertThat(auditorAware.getCurrentAuditor()).contains(publicId);
    }

    @Test
    @DisplayName("a subject with no profile yet leaves the column null")
    void returnsEmptyWhenTheSubjectHasNoPlayer() {
        when(playerRepository.findPublicIdByZitadelUserId(SUBJECT)).thenReturn(Optional.empty());
        authenticateWith(jwtToken());

        assertThat(auditorAware.getCurrentAuditor()).isEmpty();
    }

    private static JwtAuthenticationToken jwtToken() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT)
                .build();
        return new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("player"));
    }

    private static void authenticateWith(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
