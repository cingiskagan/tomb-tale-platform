package com.tombtale.serviceplayer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Player Service.
 * <p>
 * Configures this service as a stateless OAuth2 Resource Server that validates
 * JWTs issued by Zitadel. All endpoints require authentication except:
 * - Actuator health endpoint (for Docker/K8s health checks)
 * - Public GET endpoints (if any, for unauthenticated read access)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Bean
    @SuppressWarnings({ "java:S112", "java:S1130" }) // Exception type imposed by Spring
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS using the corsConfigurationSource bean
                .cors(org.springframework.security.config.Customizer.withDefaults())

                // Disable CSRF — stateless JWT-based API, no browser sessions
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management — no server-side sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow actuator health checks without authentication
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Configure as OAuth2 Resource Server using JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    // Uses the issuer-uri from application.yml to auto-discover
                    // the JWKS endpoint from Zitadel's .well-known/openid-configuration
                }));

        return http.build();
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
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
