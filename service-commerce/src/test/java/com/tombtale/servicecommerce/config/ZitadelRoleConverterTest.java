package com.tombtale.servicecommerce.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ZitadelRoleConverter} claim parsing.
 *
 * <p>
 * A plain unit test — no Spring context, no MockMvc. The converter takes a
 * {@link Jwt} and returns authorities, so it can be called directly. Tokens
 * here are unsigned stubs; signature checking happens in the decoder, long
 * before this class runs.
 *
 * <p>
 * The last two cases cover input the security slice test can never produce,
 * because its helper always builds a well-formed claim.
 */
class ZitadelRoleConverterTest {

    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String TOKEN_VALUE = "fake";
    private static final String ALG_KEY = "alg";
    private static final String ALG_VALUE = "none";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ROLE_PLAYER = "player";

    /**
     * Verifies the production claim shape: each map key becomes an authority,
     * and the map values (org id to domain) are ignored.
     */
    @Test
    void mapShapedClaimYieldsOneAuthorityPerKey() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim(ZITADEL_ROLES_CLAIM, Map.of(ROLE_PLATFORM_ADMIN, Map.of(), ROLE_PLAYER, Map.of()))
                .build();

        Collection<GrantedAuthority> authorities = new ZitadelRoleConverter().convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(ROLE_PLATFORM_ADMIN, ROLE_PLAYER);
    }

    /**
     * Verifies the alternate list shape is also accepted, so a change in how
     * Zitadel serialises the claim does not silently drop every role.
     */
    @Test
    void collectionShapedClaimYieldsOneAuthorityPerElement() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim(ZITADEL_ROLES_CLAIM, List.of(ROLE_PLATFORM_ADMIN, ROLE_PLAYER))
                .build();

        Collection<GrantedAuthority> authorities = new ZitadelRoleConverter().convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(ROLE_PLATFORM_ADMIN, ROLE_PLAYER);
    }

    /**
     * A valid token from a user with no project roles must yield an empty
     * collection rather than throwing — otherwise those users would get 500s
     * instead of 403s. The subject only satisfies the builder, which rejects
     * a token with no claims at all.
     */
    @Test
    void absentClaimYieldsNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .subject("zitadel-sub-314159")
                .build();

        Collection<GrantedAuthority> authorities = new ZitadelRoleConverter().convert(jwt);

        assertThat(authorities).isEmpty();
    }

    /**
     * Pins that Zitadel roles are added to the default converter's
     * scope-derived authorities rather than replacing them. Nothing guards on
     * {@code SCOPE_} authorities today, so this test is what would notice if
     * that merge were dropped.
     */
    @Test
    void zitadelRolesMergeWithDefaultScopeAuthorities() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim("scope", "openid profile")
                .claim(ZITADEL_ROLES_CLAIM, Map.of(ROLE_PLATFORM_ADMIN, Map.of()))
                .build();

        Collection<GrantedAuthority> authorities = new ZitadelRoleConverter().convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_profile", ROLE_PLATFORM_ADMIN);
    }

    /**
     * A claim of an unexpected type is skipped, not trusted and not fatal.
     * The value here is a real role name, so it is the shape being rejected,
     * not the content: malformed input fails closed.
     */
    @Test
    void nonCollectionClaimShapeIsIgnored() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header(ALG_KEY, ALG_VALUE)
                .claim(ZITADEL_ROLES_CLAIM, ROLE_PLATFORM_ADMIN)
                .build();

        Collection<GrantedAuthority> authorities = new ZitadelRoleConverter().convert(jwt);

        assertThat(authorities).isEmpty();
    }
}
