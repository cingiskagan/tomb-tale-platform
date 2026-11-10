package com.tombtale.serviceinventory.config;

import com.tombtale.commons.security.PlatformCorsPolicy;
import com.tombtale.commons.security.PublicIdClaimValidator;
import com.tombtale.commons.security.RoleClaimConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The same rules as service-commerce: everything needs a Keycloak JWT except the health and info probes.
 * Reading an icon is the one API call that needs no token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    /**
     * Stateless and CSRF-free, because callers send a bearer token and no cookie.
     */
    @Bean
    @SuppressWarnings("java:S112") // Spring's HttpSecurity API requires throws Exception
    public SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // An icon is public, so the game and an img tag load it with no token.
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/icons/*").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Turns the token's {@code roles} claim into authorities. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RoleClaimConverter());
        return converter;
    }

    /** Boot adds this to its decoder, so a malformed {@code public_id} fails as a 401. */
    @Bean
    public OAuth2TokenValidator<Jwt> publicIdClaimValidator() {
        return new PublicIdClaimValidator();
    }

    /** Serves the CORS rules that every service shares. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return PlatformCorsPolicy.forOrigins(allowedOrigins).toCorsConfigurationSource();
    }
}
