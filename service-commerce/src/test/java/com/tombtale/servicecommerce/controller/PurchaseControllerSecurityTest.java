package com.tombtale.servicecommerce.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tombtale.servicecommerce.config.SecurityConfig;
import com.tombtale.servicecommerce.config.ZitadelRoleConverter;
import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.service.PurchaseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Web-slice security tests for {@link PurchaseController}.
 */
@WebMvcTest(PurchaseController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
// PMD suppressions — each a deliberate false-positive call:
// - TooManyStaticImports / TooManyMethods: inherent to an 18-case MockMvc
// matrix.
// - UnitTestShouldIncludeAssert: PMD does not recognise MockMvc's
// andExpect(...) as an assertion.
// - LinguisticNaming: the getById* methods are test names, not getters
// returning a value.
// - UseConcurrentHashMap: the token helper's LinkedHashMap is a local,
// single-threaded builder.
@SuppressWarnings({
                "PMD.TooManyStaticImports",
                "PMD.TooManyMethods",
                "PMD.UnitTestShouldIncludeAssert",
                "PMD.LinguisticNaming",
                "PMD.UseConcurrentHashMap" })
class PurchaseControllerSecurityTest {

        private static final String PURCHASES_URL = "/api/v1/purchases";
        private static final String ZITADEL_ROLES_CLAIM = "urn:zitadel:iam:org:project:roles";
        private static final String ROLE_PLAYER = "player";
        private static final String ROLE_GAME_MASTER = "game_master";
        private static final String ROLE_PLATFORM_ADMIN = "platform_admin";
        private static final String SUBJECT = "zitadel-sub-314159";
        private static final String PLAYER_ID = "player-001";
        private static final UUID PURCHASE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        private static final String VALID_CREATE_BODY = """
                        {
                        "playerId": "player-001",
                        "itemCode": "SWORD_IRON",
                        "quantity": 2,
                        "unitPrice": 150.00
                        }
                        """;

        private static final String VALID_UPDATE_BODY = """
                        {
                        "quantity": 2
                        }
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PurchaseService purchaseService;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        /**
         * Builds a request post-processor simulating a Zitadel JWT with the given
         * project roles.
         *
         * <p>
         * Roles go into Zitadel's roles claim, the way real tokens carry them. The
         * production claim value is {@code {"role_name": {"org_id": "org_domain"}}};
         * only the keys matter to the converter, so tests use empty maps.
         *
         * <p>
         * Authorities are derived by running the real {@link ZitadelRoleConverter}
         * over that claim, so the claim is the single source of truth here exactly
         * as it is in production. The {@code jwt()} post-processor still bypasses
         * the decoder — no token is signed or validated — but everything from claim
         * parsing onwards is the production path.
         */
        private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
                Map<String, Object> rolesClaim = new LinkedHashMap<>();
                for (String role : roles) {
                        rolesClaim.put(role, Map.of());
                }
                return jwt()
                                .jwt(token -> token.subject(SUBJECT).claim(ZITADEL_ROLES_CLAIM, rolesClaim))
                                .authorities(new ZitadelRoleConverter());
        }

        private static PurchaseResponse aPurchaseResponse() {
                return new PurchaseResponse(
                                PURCHASE_ID,
                                PLAYER_ID,
                                "SWORD_IRON",
                                2,
                                new BigDecimal("150.00"),
                                new BigDecimal("300.00"),
                                PurchaseStatus.PENDING,
                                Instant.now());
        }

        @Test
        void createAsAnonymousReturns401() throws Exception {
                mockMvc.perform(post(PURCHASES_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_BODY))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void createAsPlayerReturns403() throws Exception {
                mockMvc.perform(post(PURCHASES_URL)
                                .with(tokenWithRoles(ROLE_PLAYER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_BODY))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void createAsGameMasterReturns403() throws Exception {
                mockMvc.perform(post(PURCHASES_URL)
                                .with(tokenWithRoles(ROLE_GAME_MASTER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_BODY))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void createAsAdminReturns201WithPlayerIdFromBody() throws Exception {
                when(purchaseService.createPurchase(any())).thenReturn(aPurchaseResponse());

                mockMvc.perform(post(PURCHASES_URL)
                                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_CREATE_BODY))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.playerId").value(PLAYER_ID));

                ArgumentCaptor<CreatePurchaseRequest> captor = ArgumentCaptor.forClass(CreatePurchaseRequest.class);
                verify(purchaseService).createPurchase(captor.capture());
                assertThat(captor.getValue().playerId()).isEqualTo(PLAYER_ID);
        }

        @Test
        void listAsAnonymousReturns401() throws Exception {
                mockMvc.perform(get(PURCHASES_URL))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void listAsPlayerReturns403() throws Exception {
                mockMvc.perform(get(PURCHASES_URL)
                                .with(tokenWithRoles(ROLE_PLAYER)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void listAsGameMasterReturns200() throws Exception {
                when(purchaseService.listPurchases(any(), any())).thenReturn(Page.empty());

                mockMvc.perform(get(PURCHASES_URL)
                                .with(tokenWithRoles(ROLE_GAME_MASTER)))
                                .andExpect(status().isOk());
        }

        @Test
        void getByIdAsAnonymousReturns401() throws Exception {
                mockMvc.perform(get(PURCHASES_URL + "/" + PURCHASE_ID))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void getByIdAsPlayerReturns403() throws Exception {
                mockMvc.perform(get(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_PLAYER)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void getByIdAsGameMasterReturns200() throws Exception {
                when(purchaseService.findPurchaseById(any())).thenReturn(aPurchaseResponse());

                mockMvc.perform(get(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_GAME_MASTER)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(PURCHASE_ID.toString()));
        }

        @Test
        void updateAsAnonymousReturns401() throws Exception {
                mockMvc.perform(put(PURCHASES_URL + "/" + PURCHASE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void updateAsPlayerReturns403() throws Exception {
                mockMvc.perform(put(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_PLAYER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void updateAsGameMasterReturns403() throws Exception {
                mockMvc.perform(put(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_GAME_MASTER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void updateAsAdminReturns200() throws Exception {
                when(purchaseService.updatePurchase(any(), any())).thenReturn(aPurchaseResponse());

                mockMvc.perform(put(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(PURCHASE_ID.toString()));
        }

        @Test
        void deleteAsAnonymousReturns401() throws Exception {
                mockMvc.perform(delete(PURCHASES_URL + "/" + PURCHASE_ID))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void deleteAsPlayerReturns403() throws Exception {
                mockMvc.perform(delete(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_PLAYER)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void deleteAsGameMasterReturns403() throws Exception {
                mockMvc.perform(delete(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_GAME_MASTER)))
                                .andExpect(status().isForbidden());

                verifyNoInteractions(purchaseService);
        }

        @Test
        void deleteAsAdminReturns204() throws Exception {
                mockMvc.perform(delete(PURCHASES_URL + "/" + PURCHASE_ID)
                                .with(tokenWithRoles(ROLE_PLATFORM_ADMIN)))
                                .andExpect(status().isNoContent());

                verify(purchaseService).deletePurchase(PURCHASE_ID);
        }
}
