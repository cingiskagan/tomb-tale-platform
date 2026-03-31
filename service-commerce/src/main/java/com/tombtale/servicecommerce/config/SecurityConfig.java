package com.tombtale.servicecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Global security configuration for the commerce service.
 *
 * <p>
 * <b>Current state (early development):</b> all endpoints are public
 * to allow Swagger UI testing without a Zitadel token. JWT authentication
 * will be re-enabled once the frontend integrates login.
 *
 * <p>Key decisions:
 * <ul>
 *   <li><b>CSRF disabled</b> — this API is stateless and will eventually use
 *       {@code Authorization: Bearer} headers, not cookies.</li>
 *   <li><b>Stateless sessions</b> — no server-side session is created.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the main security filter chain for HTTP requests.
     *
     * <p>All paths are currently permitted (no authentication required).
     * When JWT authentication is re-enabled, restrict
     * {@code anyRequest().authenticated()} and add
     * {@code .oauth2ResourceServer(…)} back.
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
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(org.springframework.security.config.Customizer.withDefaults()));
        return http.build();
    }
}
