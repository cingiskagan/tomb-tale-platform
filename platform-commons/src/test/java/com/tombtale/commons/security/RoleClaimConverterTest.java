package com.tombtale.commons.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RoleClaimConverter} called directly, with no Spring context. The tokens
 * are unsigned stubs, because the decoder checks signatures before this runs.
 */
class RoleClaimConverterTest {

    private static final String TOKEN_VALUE = "fake";
    private static final String ALG_KEY = "alg";
    private static final String ALG_VALUE = "none";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ROLE_PLAYER = "player";

    private Jwt tokenWithRoles(Object roles) {
        return Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim(RoleClaimConverter.ROLES_CLAIM, roles)
                .build();
    }

    @Test
    void listClaimYieldsOneAuthorityPerElement() {
        Collection<GrantedAuthority> authorities = new RoleClaimConverter()
                .convert(tokenWithRoles(List.of(ROLE_PLATFORM_ADMIN, ROLE_PLAYER)));

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(ROLE_PLATFORM_ADMIN, ROLE_PLAYER);
    }

    /** A user with no roles gets a 403 from method security, not a 500 from here. */
    @Test
    void absentClaimYieldsNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .subject("keycloak-sub-314159")
                .build();

        assertThat(new RoleClaimConverter().convert(jwt)).isEmpty();
    }

    /** Nothing guards on {@code SCOPE_} today, so this is what notices a dropped merge. */
    @Test
    void rolesMergeWithDefaultScopeAuthorities() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim("scope", "openid profile")
                .claim(RoleClaimConverter.ROLES_CLAIM, List.of(ROLE_PLATFORM_ADMIN))
                .build();

        assertThat(new RoleClaimConverter().convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_profile", ROLE_PLATFORM_ADMIN);
    }

    /** A real role name in the wrong shape still grants nothing: malformed input fails closed. */
    @Test
    void stringClaimIsIgnored() {
        assertThat(new RoleClaimConverter().convert(tokenWithRoles(ROLE_PLATFORM_ADMIN))).isEmpty();
    }

    /** The shape Zitadel used. A token from the old provider grants nothing here. */
    @Test
    void mapClaimIsIgnored() {
        assertThat(new RoleClaimConverter().convert(tokenWithRoles(Map.of(ROLE_PLATFORM_ADMIN, Map.of())))).isEmpty();
    }
}
