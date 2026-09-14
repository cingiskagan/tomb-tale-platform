package com.tombtale.commons.security;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * The CORS rules every Tomb Tale service serves the portal with.
 *
 * <p>
 * This used to be a copy of the same twenty lines in each service's
 * {@code SecurityConfig}, and the copies had already drifted — player allowed
 * {@code PATCH}, commerce did not, so the same request succeeded against one
 * service and failed against the other. One policy object removes the drift
 * by removing the second copy.
 *
 * <p>
 * It is a value object rather than a {@code @Configuration} on purpose: the
 * wildcard-origin check below is logic, and logic in a wiring package is
 * excluded from coverage. Each service still owns the
 * {@code CorsConfigurationSource} bean; it just builds it from here.
 */
public record PlatformCorsPolicy(List<String> allowedOrigins) {

    /** Methods the portal may call. {@code PATCH} is here because C3 moves updates to it. */
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Cache-Control", "Content-Type");

    private static final List<String> EXPOSED_HEADERS = List.of("Authorization");

    private static final String ALL_PATHS = "/**";

    /**
     * Validates the origins and keeps an immutable copy of them.
     *
     * @throws IllegalArgumentException if {@code *} appears among the origins
     */
    public PlatformCorsPolicy {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed origin is required.");
        }
        if (allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("Wildcard origins ('*') cannot be used when "
                    + "credentials are enabled. Please specify exact origins in application.yml.");
        }
        allowedOrigins = List.copyOf(allowedOrigins);
    }

    /**
     * Convenience factory for the {@code String[]} shape Spring's
     * {@code @Value} binding produces.
     *
     * @param allowedOrigins exact origins, no wildcards
     * @return the policy
     */
    public static PlatformCorsPolicy forOrigins(String... allowedOrigins) {
        return new PlatformCorsPolicy(Arrays.asList(allowedOrigins));
    }

    /**
     * Builds the source Spring Security's CORS filter consults, applying this
     * policy to every path.
     *
     * @return a configured {@link CorsConfigurationSource}
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
