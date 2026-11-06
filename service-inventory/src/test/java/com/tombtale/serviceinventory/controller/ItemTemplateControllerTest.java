package com.tombtale.serviceinventory.controller;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
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
 * The catalog endpoints from the token to the row, on a real Postgres, errors in the platform's envelope.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class ItemTemplateControllerTest extends PostgresTestBase {

    private static final String TEMPLATES_URL = "/api/v1/inventory/templates";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final String PROBLEM_TITLE = "$.title";
    private static final int PHYSICAL_DAMAGE = 12;

    private static final String SWORD = """
            {
              "code": "SWORD_IRON",
              "name": "Iron Sword",
              "itemType": "ONE_HANDED_WEAPON",
              "price": 150,
              "baseStats": {"PHYSICAL_DAMAGE": %d, "CRIT_CHANCE": 3},
              "assetKey": "Items/Weapons/SwordIron"
            }
            """.formatted(PHYSICAL_DAMAGE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @AfterEach
    void removeCommittedTemplates() {
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void anAdminCreatesATemplateThatDoesNotStack() throws Exception {
        String json = create(SWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SWORD_IRON"))
                .andExpect(jsonPath("$.maxStackSize").value(1))
                .andExpect(jsonPath("$.baseStats.PHYSICAL_DAMAGE").value(PHYSICAL_DAMAGE))
                .andExpect(jsonPath("$.retired").value(false))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();

        assertThat(itemTemplateRepository.findByPublicId(UUID.fromString(JsonPath.read(json, "$.publicId"))))
                .hasValueSatisfying(template -> assertThat(template.getCreatedBy()).isEqualTo(ADMIN));
    }

    @Test
    void aTakenCodeAnswers409() throws Exception {
        create(SWORD);

        create(SWORD)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Duplicate Item Code"));
    }

    @Test
    void aStatOutsideTheFifteenAnswers400() throws Exception {
        create(SWORD.replace("CRIT_CHANCE", "GOLD_FIND"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unknown Stat"))
                .andExpect(jsonPath("$.detail").value("Unknown stat: GOLD_FIND"));
        assertThat(itemTemplateRepository.count()).isZero();
    }

    @Test
    void anInvalidFieldAnswers400NamingIt() throws Exception {
        create(SWORD.replace("SWORD_IRON", "sword iron"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.code").exists());
    }

    @Test
    void anEditFromTheCurrentVersionSavesAndAStaleOneAnswers409() throws Exception {
        String id = idOf(create(SWORD));

        edit(id, "Sharp Iron Sword", 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sharp Iron Sword"))
                .andExpect(jsonPath("$.itemType").value("ONE_HANDED_WEAPON"))
                .andExpect(jsonPath("$.version").value(1));

        edit(id, "Blunt Iron Sword", 0)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Conflict"));
        mockMvc.perform(get(TEMPLATES_URL + "/" + id).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.name").value("Sharp Iron Sword"));
    }

    @Test
    void anUnknownTemplateAnswers404() throws Exception {
        mockMvc.perform(get(TEMPLATES_URL + "/" + UUID.randomUUID()).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aRetiredTemplateStaysInTheListAndRetiringAgainChangesNothing() throws Exception {
        String id = idOf(create(SWORD));

        retire(id).andExpect(status().isOk()).andExpect(jsonPath("$.retired").value(true));
        retire(id).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get(TEMPLATES_URL).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].retired").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void theListIsPagedAndSortedByCode() throws Exception {
        create(SWORD);
        create(SWORD.replace("SWORD_IRON", "AXE_IRON"));

        mockMvc.perform(get(TEMPLATES_URL).param("size", "1").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("AXE_IRON"))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void aSortOutsideTheListAnswers400() throws Exception {
        mockMvc.perform(get(TEMPLATES_URL).param("sort", "baseStats").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Invalid Sort Field"));
    }

    @Test
    void aGameMasterAndAPlayerAreRefused() throws Exception {
        for (String role : List.of(RoleConstants.GAME_MASTER, RoleConstants.PLAYER)) {
            mockMvc.perform(get(TEMPLATES_URL).with(tokenWithRoles(role))).andExpect(status().isForbidden());
            mockMvc.perform(post(TEMPLATES_URL).with(tokenWithRoles(role))
                            .contentType(MediaType.APPLICATION_JSON).content(SWORD))
                    .andExpect(status().isForbidden());
        }
        assertThat(itemTemplateRepository.count()).isZero();
    }

    @Test
    void aCallWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(get(TEMPLATES_URL)).andExpect(status().isUnauthorized());
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(TEMPLATES_URL)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions edit(String id, String name, int version) throws Exception {
        return mockMvc.perform(put(TEMPLATES_URL + "/" + id)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "price": 200,
                          "baseStats": {"PHYSICAL_DAMAGE": 14},
                          "assetKey": "Items/Weapons/SwordIron",
                          "version": %d
                        }
                        """.formatted(name, version)));
    }

    private ResultActions retire(String id) throws Exception {
        return mockMvc.perform(post(TEMPLATES_URL + "/" + id + "/retire")
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
