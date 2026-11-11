package com.tombtale.serviceinventory.controller;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.repository.DealerRepository;
import com.tombtale.serviceinventory.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The dealer endpoints from the token to the row, on a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class DealerControllerTest extends PostgresTestBase {

    private static final String DEALERS_URL = "/api/v1/inventory/dealers";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final String PROBLEM_TITLE = "$.title";
    private static final String CODE = "TOWN_BLACKSMITH";
    private static final String BLACKSMITH = """
            {"code": "%s", "name": "Blacksmith"}
            """.formatted(CODE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DealerRepository dealerRepository;

    @AfterEach
    void removeCommittedDealers() {
        dealerRepository.deleteAllInBatch();
    }

    @Test
    void anAdminCreatesADealerAndATakenCodeAnswers409() throws Exception {
        String json = create(BLACKSMITH)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(CODE))
                .andExpect(jsonPath("$.retired").value(false))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        assertThat(dealerRepository.findByPublicId(UUID.fromString(JsonPath.read(json, "$.publicId"))))
                .hasValueSatisfying(dealer -> assertThat(dealer.getCreatedBy()).isEqualTo(ADMIN));

        create(BLACKSMITH)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Duplicate Code"))
                .andExpect(jsonPath("$.detail").value("Another dealer already has the code " + CODE));
    }

    @Test
    void aRetiredDealerKeepsItsCode() throws Exception {
        String id = idOf(create(BLACKSMITH));

        retire(id).andExpect(status().isOk()).andExpect(jsonPath("$.retired").value(true));
        retire(id).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));

        create(BLACKSMITH).andExpect(status().isConflict());
        mockMvc.perform(get(DEALERS_URL).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.content[0].retired").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void anEditFromTheCurrentVersionSavesAndAStaleOneAnswers409() throws Exception {
        String id = idOf(create(BLACKSMITH));

        edit(id, "Old Blacksmith", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Old Blacksmith"))
                .andExpect(jsonPath("$.code").value(CODE))
                .andExpect(jsonPath("$.version").value(1));
        edit(id, "Young Blacksmith", 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Conflict"));

        mockMvc.perform(get(DEALERS_URL + "/" + id).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.name").value("Old Blacksmith"));
    }

    @Test
    void theListIsPagedByCodeAndAnUnknownDealerAnswers404() throws Exception {
        create(BLACKSMITH);
        create(BLACKSMITH.replace(CODE, "TOWN_ALCHEMIST"));

        mockMvc.perform(get(DEALERS_URL).param("size", "1").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("TOWN_ALCHEMIST"))
                .andExpect(jsonPath("$.page.totalPages").value(2));
        mockMvc.perform(get(DEALERS_URL + "/" + UUID.randomUUID()).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Not Found"));
    }

    @Test
    void anInvalidCodeAnswers400NamingIt() throws Exception {
        create(BLACKSMITH.replace(CODE, "town blacksmith"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.code").exists());
    }

    @Test
    void aGameMasterAndAPlayerAreRefused() throws Exception {
        for (String role : List.of(RoleConstants.GAME_MASTER, RoleConstants.PLAYER)) {
            mockMvc.perform(get(DEALERS_URL).with(tokenWithRoles(role))).andExpect(status().isForbidden());
            mockMvc.perform(post(DEALERS_URL).with(tokenWithRoles(role))
                            .contentType(MediaType.APPLICATION_JSON).content(BLACKSMITH))
                    .andExpect(status().isForbidden());
        }
        assertThat(dealerRepository.count()).isZero();
    }

    @Test
    void aCallWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(get(DEALERS_URL)).andExpect(status().isUnauthorized());
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(DEALERS_URL)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions edit(String id, String name, int version) throws Exception {
        return mockMvc.perform(put(DEALERS_URL + "/" + id)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "version": %d}
                        """.formatted(name, version)));
    }

    private ResultActions retire(String id) throws Exception {
        return mockMvc.perform(post(DEALERS_URL + "/" + id + "/retire")
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)));
    }

    private static String idOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.publicId");
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
