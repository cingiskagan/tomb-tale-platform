package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Custom stand entries with the setting on, as {@code .env.example} has it for the local stack.
 */
@SpringBootTest(properties = "inventory.custom-stand-items.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyStaticImports"})
class CustomStandEntryControllerTest extends FixedAuthorTestBase {

    private static final String STAND_URL = "/api/v1/inventory/stands/TOWN_JEWELER";
    private static final UUID ADMIN = UUID.fromString("0b9b7c2e-5f4d-4c3a-8e1f-7a6b5c4d3e2f");
    private static final int FIRE_DAMAGE = 40;
    private static final long RING_PRICE = 10L;
    /** Red is 243 times gray. */
    private static final long RED_RING_PRICE = 2430L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private StandEntryRepository standEntryRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    private ItemTemplate ring;

    @BeforeEach
    void aJewelerAndARing() {
        dealerRepository.save(Dealer.builder().code("TOWN_JEWELER").name("Jeweler").build());
        ring = itemTemplateRepository.save(ItemTemplates.aTemplate("RING_GOLD").price(RING_PRICE).build());
    }

    @AfterEach
    void removeCommittedRows() {
        standEntryRepository.deleteAllInBatch();
        dealerRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void aCustomEntryShowsTheBonusAnAdminChoseAtItsRaritysPrice() throws Exception {
        addCustomEntry("FIRE_DAMAGE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bonus.FIRE_DAMAGE").value(FIRE_DAMAGE));

        mockMvc.perform(get(STAND_URL).with(tokenWithRoles(RoleConstants.PLAYER)))
                .andExpect(jsonPath("$.entries[0].rarity").value("RED"))
                .andExpect(jsonPath("$.entries[0].bonus.FIRE_DAMAGE").value(FIRE_DAMAGE))
                .andExpect(jsonPath("$.entries[0].price").value(RED_RING_PRICE));
        mockMvc.perform(get("/api/v1/inventory/settings").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.customStandItemsEnabled").value(true));
    }

    @Test
    void aBonusStatOutsideTheFifteenAnswers400() throws Exception {
        addCustomEntry("GOLD_FIND")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unknown Stat"));
    }

    private ResultActions addCustomEntry(String stat) throws Exception {
        return mockMvc.perform(post(STAND_URL + "/entries")
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"templateId": "%s", "rarity": "RED", "bonus": {"%s": %d}}
                        """.formatted(ring.getPublicId(), stat, FIRE_DAMAGE)));
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
