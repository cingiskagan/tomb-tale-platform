package com.tombtale.serviceinventory.controller;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.repository.ItemSetRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The item set endpoints, and a template joining a set, on a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class ItemSetControllerTest extends PostgresTestBase {

    private static final String SETS_URL = "/api/v1/inventory/sets";
    private static final String TEMPLATES_URL = "/api/v1/inventory/templates";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final String PROBLEM_TITLE = "$.title";
    private static final String ITEM_SET_ID = "$.itemSetId";

    private static final int MAX_HP_BONUS = 50;
    private static final int EDITED_ARMOR_BONUS = 25;

    private static final String IRON_SET = """
            {"code": "IRON_SET", "name": "Iron Set", "bonus": {"ARMOR": 20, "MAX_HP": %d}}
            """.formatted(MAX_HP_BONUS);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemSetRepository itemSetRepository;

    @AfterEach
    void removeCommittedRows() {
        itemTemplateRepository.deleteAllInBatch();
        itemSetRepository.deleteAllInBatch();
    }

    @Test
    void anAdminCreatesASetAndATakenCodeAnswers409() throws Exception {
        createSet(IRON_SET)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IRON_SET"))
                .andExpect(jsonPath("$.bonus.MAX_HP").value(MAX_HP_BONUS))
                .andExpect(jsonPath("$.version").value(0));

        createSet(IRON_SET)
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Duplicate Code"));
    }

    @Test
    void aBonusStatOutsideTheFifteenAnswers400() throws Exception {
        createSet(IRON_SET.replace("MAX_HP", "GOLD_FIND"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unknown Stat"));
    }

    @Test
    void anEditFromTheCurrentVersionSavesAndAStaleOneAnswers409() throws Exception {
        String id = idOf(createSet(IRON_SET));

        editSet(id, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonus.ARMOR").value(EDITED_ARMOR_BONUS))
                .andExpect(jsonPath("$.version").value(1));
        editSet(id, 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Conflict"));
    }

    @Test
    void theListIsPagedAndAnUnknownSetAnswers404() throws Exception {
        createSet(IRON_SET);

        mockMvc.perform(get(SETS_URL).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("IRON_SET"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(get(SETS_URL + "/" + UUID.randomUUID()).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aGameMasterIsRefused() throws Exception {
        mockMvc.perform(get(SETS_URL).with(tokenWithRoles(RoleConstants.GAME_MASTER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTemplateJoinsASetAndLeavesIt() throws Exception {
        String setId = idOf(createSet(IRON_SET));

        String templateId = idOf(createTemplate(setId).andExpect(jsonPath(ITEM_SET_ID).value(setId)));
        mockMvc.perform(get(TEMPLATES_URL).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.content[0].itemSetId").value(setId));

        mockMvc.perform(put(TEMPLATES_URL + "/" + templateId)
                        .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Iron Helmet", "price": 80, "baseStats": {"ARMOR": 5},
                                 "assetKey": "Items/Armor/IronHelmet", "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ITEM_SET_ID).doesNotExist());
    }

    @Test
    void aTemplateInASetThatDoesNotExistAnswers400() throws Exception {
        createTemplate(UUID.randomUUID().toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unknown Item Set"));
    }

    private ResultActions createSet(String body) throws Exception {
        return mockMvc.perform(post(SETS_URL)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions editSet(String id, int version) throws Exception {
        return mockMvc.perform(put(SETS_URL + "/" + id)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Iron Set", "bonus": {"ARMOR": %d}, "version": %d}
                        """.formatted(EDITED_ARMOR_BONUS, version)));
    }

    private ResultActions createTemplate(String itemSetId) throws Exception {
        return mockMvc.perform(post(TEMPLATES_URL)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "HELMET_IRON", "name": "Iron Helmet", "itemType": "HELMET", "price": 80,
                         "baseStats": {"ARMOR": 5}, "assetKey": "Items/Armor/IronHelmet", "itemSetId": "%s"}
                        """.formatted(itemSetId)));
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
