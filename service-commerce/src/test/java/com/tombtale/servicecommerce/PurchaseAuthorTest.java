package com.tombtale.servicecommerce;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.servicecommerce.entity.Purchase;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who a purchase names as its author, from the token through the auditor to the row. Both tokens carry
 * {@code platform_admin}, so a 403 here comes from the auditor and not from {@code @PreAuthorize}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseAuthorTest extends PostgresTestBase {

    private static final String PURCHASES_URL = "/api/v1/purchases";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");

    private static final String NEW_PURCHASE_BODY = """
            {
              "playerId": "aaaaaaaa-0000-4000-8000-00000000beef",
              "itemCode": "SWORD_IRON",
              "quantity": 2,
              "unitPrice": 150.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @AfterEach
    void removeCommittedPurchases() {
        purchaseRepository.deleteAllInBatch();
    }

    @Test
    void anAdminsPurchaseNamesTheAdmin() throws Exception {
        String json = mockMvc.perform(post(PURCHASES_URL)
                .with(adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_PURCHASE_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Purchase purchase = purchaseRepository
                .findByPublicId(UUID.fromString(JsonPath.read(json, "$.publicId"))).orElseThrow();

        assertThat(purchase.getCreatedBy()).isEqualTo(ADMIN);
        assertThat(purchase.getUpdatedBy()).isEqualTo(ADMIN);
    }

    @Test
    void aTokenWithoutThePublicIdIsRefused() throws Exception {
        mockMvc.perform(post(PURCHASES_URL)
                .with(adminTokenWithoutPublicId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_PURCHASE_BODY))
                .andExpect(status().isForbidden());

        assertThat(purchaseRepository.count()).isZero();
    }

    private static JwtRequestPostProcessor adminToken() {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of("platform_admin")))
                .authorities(new RoleClaimConverter());
    }

    private static JwtRequestPostProcessor adminTokenWithoutPublicId() {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of("platform_admin")))
                .authorities(new RoleClaimConverter());
    }
}
