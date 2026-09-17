package com.tombtale.commons.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Extracts custom project roles from Zitadel's JWT claim and converts them
 * into Spring Security GrantedAuthorities.
 *
 * <p>
 * One copy for the whole platform. Both services wire it into their
 * {@code JwtAuthenticationConverter}, so a change in how Zitadel serialises
 * the claim cannot be fixed in one service and forgotten in the other.
 * The role names it produces are listed in {@link RoleConstants}.
 */
public class ZitadelRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final Collection<GrantedAuthority> authorities = new ArrayList<>(
                defaultGrantedAuthoritiesConverter.convert(jwt));

        Object rolesObj = jwt.getClaims().get(ZITADEL_ROLES_CLAIM);

        if (rolesObj instanceof Map<?, ?> rolesMap) {
            rolesMap.keySet().forEach(key -> authorities.add(new SimpleGrantedAuthority(key.toString())));
        } else if (rolesObj instanceof Collection<?> rolesList) {
            rolesList.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.toString())));
        }

        return authorities;
    }
}
