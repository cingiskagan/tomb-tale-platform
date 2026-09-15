package com.tombtale.commons.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the shared CORS policy.
 *
 * <p>
 * Two things are worth pinning: that the wildcard guard still fires, and that
 * the method list contains the verbs the portal actually sends. The second is
 * the drift this class exists to stop — commerce allowed every method except
 * {@code PATCH}, and nothing noticed until a request failed.
 */
class PlatformCorsPolicyTest {

    private static final String PORTAL_ORIGIN = "http://localhost:4200";

    private static CorsConfiguration configurationFor(PlatformCorsPolicy policy) {
        HttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/api/v1/players");
        CorsConfiguration configuration = policy.toCorsConfigurationSource().getCorsConfiguration(request);
        assertThat(configuration).isNotNull();
        return configuration;
    }

    @Test
    void allowsEveryMethodThePortalSends() {
        CorsConfiguration configuration = configurationFor(PlatformCorsPolicy.forOrigins(PORTAL_ORIGIN));

        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Test
    void carriesTheConfiguredOriginsAndAllowsCredentials() {
        CorsConfiguration configuration = configurationFor(
                PlatformCorsPolicy.forOrigins(PORTAL_ORIGIN, "https://portal.tombtale.test"));

        assertThat(configuration.getAllowedOrigins())
                .containsExactly(PORTAL_ORIGIN, "https://portal.tombtale.test");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedHeaders()).contains(HttpHeaders.AUTHORIZATION);
        assertThat(configuration.getExposedHeaders()).containsExactly(HttpHeaders.AUTHORIZATION);
    }

    /**
     * Credentials plus {@code *} is a combination browsers reject outright, so
     * the misconfiguration has to fail at startup rather than at the first
     * cross-origin request.
     */
    @Test
    void rejectsWildcardOrigin() {
        assertThatThrownBy(() -> PlatformCorsPolicy.forOrigins(PORTAL_ORIGIN, "*"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wildcard origins");
    }

    @Test
    void rejectsAnEmptyOriginList() {
        assertThatThrownBy(() -> new PlatformCorsPolicy(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one allowed origin");
    }

    /**
     * A wildcard subdomain is rejected too, not just a bare {@code *}. Spring
     * matches this list literally, so such an entry would silently match no
     * origin at all.
     */
    @Test
    void rejectsAWildcardInsideAnOrigin() {
        assertThatThrownBy(() -> PlatformCorsPolicy.forOrigins(PORTAL_ORIGIN, "https://*.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wildcard origins");
    }

    /**
     * A record's canonical constructor is public, so null is reachable. It has
     * to fail with the same message rather than an NPE from further down.
     */
    @Test
    void rejectsNullOrigins() {
        assertThatThrownBy(() -> new PlatformCorsPolicy(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one allowed origin");
    }
}
