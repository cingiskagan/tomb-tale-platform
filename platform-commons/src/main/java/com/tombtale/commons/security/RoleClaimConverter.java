package com.tombtale.commons.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Turns the token's {@code roles} list into authorities, beside the
 * {@code SCOPE_} ones Spring derives. Both services wire this one copy (ADR 0024).
 */
public class RoleClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /** The claim the realm file's role mapper writes, one realm role name per element. */
    public static final String ROLES_CLAIM = "roles";

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        final Collection<GrantedAuthority> authorities = new ArrayList<>(
                defaultGrantedAuthoritiesConverter.convert(jwt));

        // Only a list counts, so a malformed claim grants nothing.
        if (jwt.getClaims().get(ROLES_CLAIM) instanceof Collection<?> roles) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.toString())));
        }

        return authorities;
    }
}
