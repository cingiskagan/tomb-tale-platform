package com.tombtale.commons.security;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * The CORS rules every Tomb Tale service serves the portal with.
 *
 * <p>A value object rather than a {@code @Configuration}, because the wildcard
 * check is logic and a wiring package is excluded from coverage. Each service
 * still owns its {@code CorsConfigurationSource} bean and builds it from here.
 */
public record PlatformCorsPolicy(List<String> allowedOrigins) {

    /** Methods the portal may call. */
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Cache-Control", "Content-Type");

    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private static final String ALL_PATHS = "/**";

    private static final String WILDCARD = "*";

    /**
     * Validates the origins and keeps an immutable copy.
     *
     * @throws IllegalArgumentException if the list is empty or any origin contains {@code *}
     */
    public PlatformCorsPolicy {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed origin is required.");
        }
        if (allowedOrigins.stream().anyMatch(origin -> origin.contains(WILDCARD))) {
            throw new IllegalArgumentException("Wildcard origins cannot be used when credentials "
                    + "are enabled, and Spring matches this list literally, so a pattern would "
                    + "never match. Please specify exact origins in application.yml.");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    /**
     * Factory for the {@code String[]} shape {@code @Value} binding produces.
     *
     * @param allowedOrigins exact origins, no wildcards
     * @return the policy
     */
    public static PlatformCorsPolicy forOrigins(String... allowedOrigins) {
        return new PlatformCorsPolicy(Arrays.asList(allowedOrigins));
    }

    /**
     * Applies this policy to every path.
     *
     * @return the source Spring Security's CORS filter consults
     */
    public CorsConfigurationSource toCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATHS, configuration);
        return source;
    }
}
