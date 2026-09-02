package com.tombtale.serviceplayer;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.serviceplayer.config.ZitadelRoleConverter;
import com.tombtale.serviceplayer.support.PostgresTestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for service-player: the real application context, a real Postgres
 * from Testcontainers, and one request that travels the whole path from the
 * security filter chain down to a committed row.
 *
 * <p>Deliberately two tests, and it stays that way. Anything narrower belongs in
 * a slice: {@code @WebMvcTest} for status codes and JSON shape,
 * {@code @DataJpaTest} for queries. Smoke tests answer one question — is the
 * application wired together at all — and they are the slowest tests we own.
 *
 * <p>The token is built with the {@code jwt()} post-processor instead of a real
 * signed one, so {@code JwtDecoder} is the single piece of production wiring
 * these tests do not exercise. Signature and issuer checks are Spring's code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's fluent API is assembled from static imports: the request builders,
// the result matchers, the jwt post-processor and assertThat. Six is the
// natural count for one request, not a sign of a messy class.
@SuppressWarnings("PMD.TooManyStaticImports")
class ServicePlayerApplicationTests extends PostgresTestBase {

    private static final String ME_URL = "/api/v1/players/me";
    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String ROLE_PLAYER = "player";

    /** JIT provisioning gives every new player exactly one default character. */
    private static final int DEFAULT_CHARACTER_COUNT = 1;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }

    /**
     * The JIT-provisioning happy path. A subject that has never been seen gets a
     * profile and a default character on the first call, and the second call
     * returns that same profile rather than making another one.
     *
     * <p>Comparing publicId across the two calls is the part that matters. A
     * single 200 would also pass if nothing were ever committed; only the second
     * call proves the row survived the first request.
     */
    @Test
    void firstProfileCallCreatesThePlayerAndTheSecondReturnsTheSameOne() throws Exception {
        String subject = "smoke-" + UUID.randomUUID();

        MvcResult created = mockMvc.perform(get(ME_URL).with(tokenFor(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.displayName").isNotEmpty())
                .andExpect(jsonPath("$.characters", hasSize(DEFAULT_CHARACTER_COUNT)))
                .andReturn();

        String publicId = JsonPath.read(created.getResponse().getContentAsString(), "$.publicId");
        assertThat(publicId).isNotBlank();

        mockMvc.perform(get(ME_URL).with(tokenFor(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId));
    }

    /**
     * Builds a token carrying the Zitadel roles claim, converted by the same
     * {@link ZitadelRoleConverter} the application uses — so the claim shape is
     * the single source of truth here as well as in production.
     *
     * <p>Callers pass a fresh subject per run. The shared container is not wiped
     * between test classes, and both zitadelUserId and displayName are unique
     * columns, so reusing a fixed subject would fail on the second run.
     */
    private static JwtRequestPostProcessor tokenFor(String subject) {
        return jwt()
                .jwt(token -> token.subject(subject).claim(ZITADEL_ROLES_CLAIM, Map.of(ROLE_PLAYER, Map.of())))
                .authorities(new ZitadelRoleConverter());
    }
}
