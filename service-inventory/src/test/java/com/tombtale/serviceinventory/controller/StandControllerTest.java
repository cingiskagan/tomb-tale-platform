package com.tombtale.serviceinventory.controller;

import com.jayway.jsonpath.JsonPath;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.DealerRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.repository.StandEntryRepository;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;
import com.tombtale.serviceinventory.support.ItemTemplates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stands and the settings read, with custom stand items off as they are by default, on a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class StandControllerTest extends FixedAuthorTestBase {

    private static final String STAND_URL = "/api/v1/inventory/stands/TOWN_BLACKSMITH";
    private static final String ENTRIES_URL = STAND_URL + "/entries";
    private static final UUID CALLER = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final String PROBLEM_TITLE = "$.title";
    private static final String FIRST_ENTRY = "$.entries[0]";
    private static final long SWORD_PRICE = 150L;
    private static final long BLUE_SWORD_PRICE = 1350L;
    private static final int SWORD_DAMAGE = 12;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private StandEntryRepository standEntryRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    private Dealer blacksmith;
    private ItemTemplate sword;
    private ItemTemplate shield;

    @BeforeEach
    void aBlacksmithAndTwoTemplates() {
        blacksmith = dealerRepository.save(Dealer.builder().code("TOWN_BLACKSMITH").name("Blacksmith").build());
        sword = itemTemplateRepository.save(ItemTemplates.aTemplate("SWORD_IRON")
                .name("Iron Sword")
                .itemType(ItemType.ONE_HANDED_WEAPON)
                .price(SWORD_PRICE)
                .baseStats(Map.of(Stat.PHYSICAL_DAMAGE, SWORD_DAMAGE))
                .build());
        shield = itemTemplateRepository.save(ItemTemplates.aTemplate("SHIELD_IRON").itemType(ItemType.SHIELD).build());
    }

    @AfterEach
    void removeCommittedRows() {
        standEntryRepository.deleteAllInBatch();
        dealerRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void anAdminAddsAnEntryAndAPlayerSeesItPricedByItsRarity() throws Exception {
        String entryId = idOf(addEntry(sword, Rarity.BLUE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(BLUE_SWORD_PRICE))
                .andExpect(jsonPath("$.bonus").doesNotExist()));

        mockMvc.perform(get(STAND_URL).with(tokenWithRoles(RoleConstants.PLAYER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOWN_BLACKSMITH"))
                .andExpect(jsonPath("$.name").value("Blacksmith"))
                .andExpect(jsonPath(FIRST_ENTRY + ".publicId").value(entryId))
                .andExpect(jsonPath(FIRST_ENTRY + ".code").value("SWORD_IRON"))
                .andExpect(jsonPath(FIRST_ENTRY + ".name").value("Iron Sword"))
                .andExpect(jsonPath(FIRST_ENTRY + ".itemType").value("ONE_HANDED_WEAPON"))
                .andExpect(jsonPath(FIRST_ENTRY + ".iconUrl").doesNotExist())
                .andExpect(jsonPath(FIRST_ENTRY + ".baseStats.PHYSICAL_DAMAGE").value(SWORD_DAMAGE))
                .andExpect(jsonPath(FIRST_ENTRY + ".rarity").value("BLUE"))
                .andExpect(jsonPath(FIRST_ENTRY + ".price").value(BLUE_SWORD_PRICE));
    }

    @Test
    void aRetiredTemplateLeavesTheStandAndCannotJoinIt() throws Exception {
        addEntry(sword, Rarity.GRAY);
        addEntry(shield, Rarity.GREEN);

        sword.setRetired(true);
        itemTemplateRepository.save(sword);

        readStand()
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath(FIRST_ENTRY + ".code").value("SHIELD_IRON"));
        addEntry(sword, Rarity.RED)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Template Retired"));
    }

    @Test
    void aRetiredDealerSellsNothing() throws Exception {
        addEntry(sword, Rarity.GRAY);

        blacksmith.setRetired(true);
        dealerRepository.save(blacksmith);

        readStand().andExpect(status().isOk()).andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    void anAdminRemovesAnEntryOnlyFromItsOwnStand() throws Exception {
        String entryId = idOf(addEntry(sword, Rarity.GRAY));
        dealerRepository.save(Dealer.builder().code("TOWN_ALCHEMIST").name("Alchemist").build());

        mockMvc.perform(delete("/api/v1/inventory/stands/TOWN_ALCHEMIST/entries/" + entryId)
                        .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isNotFound());
        removeEntry(entryId).andExpect(status().isNoContent());

        readStand().andExpect(jsonPath("$.entries").isEmpty());
        removeEntry(entryId).andExpect(status().isNotFound());
    }

    @Test
    void aCustomEntryIsRefusedWhileTheSettingIsOff() throws Exception {
        mockMvc.perform(post(ENTRIES_URL)
                        .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": "%s", "rarity": "RED", "bonus": {"FIRE_DAMAGE": 40}}
                                """.formatted(sword.getPublicId())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Custom Stand Items Disabled"));
        assertThat(standEntryRepository.count()).isZero();

        mockMvc.perform(get("/api/v1/inventory/settings").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customStandItemsEnabled").value(false));
    }

    @Test
    void anUnknownTemplateAnswers400AndAnUnknownDealer404() throws Exception {
        mockMvc.perform(post(ENTRIES_URL)
                        .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": "%s", "rarity": "GRAY"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Unknown Item Template"));
        mockMvc.perform(get("/api/v1/inventory/stands/TOWN_BAKER").with(tokenWithRoles(RoleConstants.PLAYER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Dealer not found: TOWN_BAKER"));
    }

    @Test
    void anEntryWithoutARarityAnswers400NamingIt() throws Exception {
        mockMvc.perform(post(ENTRIES_URL)
                        .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": "%s"}
                                """.formatted(sword.getPublicId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.rarity").exists());
    }

    @Test
    void aPlayerAndAGameMasterReadAStandButChangeNone() throws Exception {
        String entryId = idOf(addEntry(sword, Rarity.GRAY));

        for (String role : List.of(RoleConstants.GAME_MASTER, RoleConstants.PLAYER)) {
            mockMvc.perform(get(STAND_URL).with(tokenWithRoles(role))).andExpect(status().isOk());
            mockMvc.perform(post(ENTRIES_URL).with(tokenWithRoles(role))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"templateId": "%s", "rarity": "GRAY"}
                                    """.formatted(sword.getPublicId())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(ENTRIES_URL + "/" + entryId).with(tokenWithRoles(role)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/inventory/settings").with(tokenWithRoles(role)))
                    .andExpect(status().isForbidden());
        }
        assertThat(standEntryRepository.count()).isOne();
    }

    @Test
    void aReadWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(get(STAND_URL)).andExpect(status().isUnauthorized());
    }

    private ResultActions addEntry(ItemTemplate template, Rarity rarity) throws Exception {
        return mockMvc.perform(post(ENTRIES_URL)
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"templateId": "%s", "rarity": "%s"}
                        """.formatted(template.getPublicId(), rarity)));
    }

    private ResultActions removeEntry(String entryId) throws Exception {
        return mockMvc.perform(delete(ENTRIES_URL + "/" + entryId).with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)));
    }

    private ResultActions readStand() throws Exception {
        return mockMvc.perform(get(STAND_URL).with(tokenWithRoles(RoleConstants.PLAYER)));
    }

    private static String idOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.publicId");
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(CALLER.toString())
                        .claim(PublicIdClaim.CLAIM, CALLER.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
