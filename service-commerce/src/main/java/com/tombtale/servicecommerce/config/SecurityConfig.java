package com.tombtale.servicecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Global security configuration for the commerce service.
 *
 * <p>
 * <b>Current state:</b> API endpoints ({@code /api/**}) require JWT
 * authentication. Swagger UI, actuator, and other paths are public
 * to support development and monitoring.
 *
 * <p>
 * Authorization works in two layers. This class configures URL-level
 * security (who may reach {@code /api/**} at all); per-endpoint role rules
 * live in {@code @PreAuthorize} annotations on the controllers, enabled by
 * {@code @EnableMethodSecurity}.
 *
 * <p>
 * Key decisions:
 * <ul>
 * <li><b>CSRF disabled</b> — this API is stateless and uses
 * {@code Authorization: Bearer} headers, not cookies.</li>
 * <li><b>Stateless sessions</b> — no server-side session is created.</li>
 * <li><b>Zitadel roles as authorities</b> — {@link ZitadelRoleConverter}
 * reads the project-roles claim so endpoints can guard with
 * {@code hasAuthority('platform_admin')} rather than {@code hasRole(…)},
 * which would expect a {@code ROLE_} prefix Zitadel never issues.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    /**
     * Configures the main security filter chain for HTTP requests.
     *
     * <p>
     * Requests to {@code /api/**} must carry a valid Zitadel JWT; anything
     * else (Swagger UI, actuator) is public. Rejected requests fail here with
     * 401 — a 403 comes from the method-security layer instead, once the
     * caller is known but lacks the required role.
     *
     * @param http The HttpSecurity builder to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @SuppressWarnings("java:S112") // Spring's HttpSecurity API requires throws Exception
    public SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Configures the JWT converter to extract custom project roles.
     *
     * <p>
     * Replaces Spring's default scope-based authority mapping with
     * {@link ZitadelRoleConverter}, which merges Zitadel project roles into
     * the authorities the default converter already derives from {@code scope}.
     *
     * @return The converter used to turn JWT claims into granted authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new ZitadelRoleConverter());
        return converter;
    }

    /**
     * Configures the CORS configuration source.
     * Allows requests from localhost:4200 (Angular dev server).
     *
     * @return The configured CorsConfigurationSource.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins);

        if (origins.contains("*")) {
            throw new IllegalArgumentException("Wildcard origins ('*') cannot be used when "
                    + "credentials are enabled. Please specify exact origins in application.yml.");
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
