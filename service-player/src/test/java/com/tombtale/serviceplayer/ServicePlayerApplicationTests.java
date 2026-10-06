package com.tombtale.serviceplayer;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.serviceplayer.support.PostgresTestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
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
 * signed one, which is the production wiring these tests do not exercise.
 * Signature and issuer checks are Spring's code.
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
    private static final String ROLE_PLAYER = "player";

    /** JIT provisioning gives every new player exactly one default character. */
    private static final int DEFAULT_CHARACTER_COUNT = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private UUID publicId;
    private String keycloakId;

    @BeforeEach
    void newPlayer() {
        publicId = UUID.randomUUID();
        keycloakId = "keycloak-" + UUID.randomUUID();
    }

    /**
     * Removes the player and its outbox row, which this class commits.
     *
     * <p>Every other test in this suite is transactional and rolls itself back.
     * These are not: {@code getOrCreatePlayer} runs with
     * {@code Propagation.NOT_SUPPORTED}, so the insert commits and outlives the
     * request. The container is shared by the whole suite, so a leftover player
     * changes the row count that {@code PlayerQueryRepositoryImplTest} asserts —
     * a failure that only appears when Surefire happens to run this class first.
     */
    @AfterEach
    void removeCommittedPlayer() {
        playerRepository.findByPublicIdWithCharacters(publicId).ifPresent(player -> {
            outboxEventRepository.deleteAll(outboxEventRepository.findAll().stream()
                    .filter(event -> event.getAggregateId().equals(player.getPublicId()))
                    .toList());
            playerRepository.delete(player);
        });
    }

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }

    /**
     * The first call creates the profile and a default character under the
     * token's {@code publicId}, keeping its {@code sub}, and the second returns
     * that same row. Only the second call proves the row survived the first request.
     */
    @Test
    void firstProfileCallCreatesThePlayerAndTheSecondReturnsTheSameOne() throws Exception {
        MvcResult created = mockMvc.perform(get(ME_URL).with(playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.displayName").isNotEmpty())
                .andExpect(jsonPath("$.characters", hasSize(DEFAULT_CHARACTER_COUNT)))
                .andReturn();

        String displayName = JsonPath.read(created.getResponse().getContentAsString(), "$.displayName");
        assertThat(displayName).isNotBlank();
        assertThat(playerRepository.findByPublicIdWithCharacters(publicId).orElseThrow().getKeycloakId())
                .isEqualTo(keycloakId);

        mockMvc.perform(get(ME_URL).with(playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(displayName));
    }

    /**
     * A token shaped like Keycloak's, converted by the application's own
     * {@link RoleClaimConverter}. The {@code publicId} is fresh per test, because
     * the shared container keeps committed rows between classes.
     */
    private JwtRequestPostProcessor playerToken() {
        return jwt()
                .jwt(token -> token.subject(keycloakId)
                        .claim(PublicIdClaim.CLAIM, publicId.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(ROLE_PLAYER)))
                .authorities(new RoleClaimConverter());
    }
}
