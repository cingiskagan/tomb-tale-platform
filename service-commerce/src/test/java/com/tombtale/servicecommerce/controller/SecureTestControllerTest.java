package com.tombtale.servicecommerce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecureTestController}.
 *
 * <p>Uses a manually constructed {@link Jwt} stub to verify claim extraction
 * without depending on Spring Boot's {@code @WebMvcTest} (which has
 * known compatibility issues in Spring Boot 4.x).
 */
class SecureTestControllerTest {

    private static final String MOCK_SUBJECT = "test-user-id-12345";

    private final SecureTestController controller = new SecureTestController();

    /**
     * Verifies the greeting contains the JWT's subject claim.
     */
    @Test
    void shouldReturnGreetingWithSubjectClaim() {
        Jwt jwt = buildJwtWithSubject(MOCK_SUBJECT);

        String result = controller.returnSecureHello(jwt);

        assertThat(result).isEqualTo("Hello, " + MOCK_SUBJECT + "!");
    }

    /**
     * Builds a minimal {@link Jwt} stub with the given subject claim.
     *
     * @param subject The subject (sub) claim value.
     * @return A valid Jwt instance for testing.
     */
    private static Jwt buildJwtWithSubject(String subject) {
        return new Jwt(
            "mock.jwt.token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            Map.of("sub", subject)
        );
    }
}
