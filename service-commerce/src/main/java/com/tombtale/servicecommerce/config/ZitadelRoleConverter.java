package com.tombtale.servicecommerce.config;

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
 */
public class ZitadelRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> defaultAuthorities = defaultGrantedAuthoritiesConverter.convert(jwt);
        final Collection<GrantedAuthority> authorities = defaultAuthorities == null 
                ? new ArrayList<>() 
                : new ArrayList<>(defaultAuthorities);

        Object rolesObj = jwt.getClaims().get(ZITADEL_ROLES_CLAIM);
        
        if (rolesObj instanceof Map<?, ?> rolesMap) {
            rolesMap.keySet().forEach(key -> 
                authorities.add(new SimpleGrantedAuthority(key.toString()))
            );
        } else if (rolesObj instanceof Collection<?> rolesList) {
             rolesList.forEach(role -> 
                authorities.add(new SimpleGrantedAuthority(role.toString()))
            );
        }

        return authorities;
    }
}
