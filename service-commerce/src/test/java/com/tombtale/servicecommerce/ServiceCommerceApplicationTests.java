package com.tombtale.servicecommerce;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.servicecommerce.config.ZitadelRoleConverter;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
import com.tombtale.servicecommerce.support.PostgresTestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for service-commerce: the real application context, a real
 * Postgres from Testcontainers, and one request that travels the whole path
 * from the security filter chain down to a committed row.
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
class ServiceCommerceApplicationTests extends PostgresTestBase {

    private static final String PURCHASES_URL = "/api/v1/purchases";
    private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
    private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
    private static final String ADMIN_SUBJECT = "smoke-admin";

    private static final String ITEM_CODE = "SWORD_IRON";
    private static final String PLAYER_ID = "smoke-player-001";

    /** quantity 2 × unitPrice 150.00, computed by the service and never sent by the client. */
    private static final String EXPECTED_TOTAL_PRICE = "300.00";

    private static final String NEW_PURCHASE_BODY = """
            {
              "playerId": "smoke-player-001",
              "itemCode": "SWORD_IRON",
              "quantity": 2,
              "unitPrice": 150.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    private UUID createdPurchaseId;

    /**
     * Removes the row this class commits.
     *
     * <p>Every other test in this suite is transactional and rolls itself back.
     * This one is not: the service transaction commits, and the container is
     * shared by the whole suite, so a leftover purchase changes the counts that
     * {@code PurchaseQueryRepositoryImplTest} asserts — a failure that only
     * appears when Surefire happens to run this class first.
     */
    @AfterEach
    void removeCommittedPurchase() {
        // No reset afterwards: JUnit builds a fresh instance per test method, so
        // the field starts null again on its own. The guard is for contextLoads,
        // which never creates anything.
        if (createdPurchaseId != null) {
            purchaseRepository.findById(createdPurchaseId).ifPresent(purchaseRepository::delete);
        }
    }

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }

    /**
     * The create-then-read happy path. An admin creates a purchase on behalf of a
     * player, and the same row comes back on a fresh request by its generated id.
     *
     * <p>Reading it back in a second request is the part that matters. The
     * response to a POST can be assembled entirely in memory; only a later GET
     * proves the row reached Postgres and maps back out again.
     *
     * <p>totalPrice is compared as a BigDecimal, not matched as JSON. The column
     * has scale 4, so the value serialises as 300.0000 — equal to 300.00 in
     * value but not as text.
     */
    @Test
    void createdPurchaseIsPersistedAndReadableById() throws Exception {
        MvcResult created = mockMvc.perform(post(PURCHASES_URL)
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_PURCHASE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.playerId").value(PLAYER_ID))
                .andExpect(jsonPath("$.status").value(PurchaseStatus.PENDING.name()))
                .andReturn();

        String json = created.getResponse().getContentAsString();

        // Read via toString rather than a typed read: the JsonPath provider may
        // return a Double or a BigDecimal depending on configuration, and both
        // render text that BigDecimal parses exactly.
        BigDecimal totalPrice = new BigDecimal(JsonPath.read(json, "$.totalPrice").toString());
        assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(EXPECTED_TOTAL_PRICE));

        String id = JsonPath.read(json, "$.id");
        createdPurchaseId = UUID.fromString(id);

        mockMvc.perform(get(PURCHASES_URL + "/{id}", id)
                .with(adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.itemCode").value(ITEM_CODE))
                .andExpect(jsonPath("$.status").value(PurchaseStatus.PENDING.name()));
    }

    /**
     * Builds a platform_admin token carrying the Zitadel roles claim, converted by
     * the same {@link ZitadelRoleConverter} the application uses — so the claim
     * shape is the single source of truth here as well as in production.
     */
    private static JwtRequestPostProcessor adminToken() {
        return jwt()
                .jwt(token -> token.subject(ADMIN_SUBJECT)
                        .claim(ZITADEL_ROLES_CLAIM, Map.of(ROLE_PLATFORM_ADMIN, Map.of())))
                .authorities(new ZitadelRoleConverter());
    }
}
