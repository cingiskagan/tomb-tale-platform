package com.tombtale.servicecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Global security configuration for the commerce service.
 *
 * <p>
 * Configures the application as a stateless OAuth2 resource server that
 * validates
 * JWT bearer tokens issued by Zitadel. Key decisions:
 * <ul>
 * <li><b>CSRF disabled</b> — this API is stateless and uses
 * {@code Authorization: Bearer}
 * headers, not cookies. CSRF attacks only exploit cookie-based authentication
 * where
 * the browser automatically attaches credentials. With JWTs sent explicitly via
 * headers,
 * there is no CSRF attack vector.</li>
 * <li><b>Stateless sessions</b> — no server-side session is created; every
 * request is
 * independently authenticated via its JWT.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the main security filter chain for HTTP requests.
     *
     * <p>
     * Public paths (no token required):
     * <ul>
     * <li>{@code /api/v1/test/**} — smoke-test / connectivity endpoints</li>
     * <li>{@code /actuator/health, /actuator/info, /actuator/metrics} — observability</li>
     * </ul>
     *
     * <p>
     * All other paths require a valid Zitadel-issued JWT.
     *
     * @param http The HttpSecurity builder to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception If an error occurs during configuration.
     */
    @Bean
    @SuppressWarnings("java:S112") // Spring's HttpSecurity API requires throws Exception
    public SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/test/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
