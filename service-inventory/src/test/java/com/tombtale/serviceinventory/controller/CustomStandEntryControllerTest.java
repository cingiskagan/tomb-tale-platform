package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.Rarity;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    /** Blue is 9 times gray. */
    private static final long BLUE_RING_PRICE = 90L;

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

    /** The bonus repeats a stat, which a rolled one may do too. */
    @Test
    void aCustomEntryShowsTheBonusAnAdminChoseAtItsRaritysPrice() throws Exception {
        addCustomEntry("FIRE_DAMAGE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bonus.length()").value(2))
                .andExpect(jsonPath("$.bonus[1].stat").value("FIRE_DAMAGE"))
                .andExpect(jsonPath("$.bonus[1].value").value(FIRE_DAMAGE));

        mockMvc.perform(get(STAND_URL).with(tokenWithRoles(RoleConstants.PLAYER)))
                .andExpect(jsonPath("$.entries[0].rarity").value("BLUE"))
                .andExpect(jsonPath("$.entries[0].bonus[0].value").value(FIRE_DAMAGE))
                .andExpect(jsonPath("$.entries[0].price").value(BLUE_RING_PRICE));
        mockMvc.perform(get("/api/v1/inventory/settings").with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN)))
                .andExpect(jsonPath("$.customStandItemsEnabled").value(true));
    }

    @Test
    void aBonusStatOutsideTheFifteenAnswers400() throws Exception {
        addCustomEntry("GOLD_FIND")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Unknown Stat"));
    }

    @Test
    void aBonusLineWithoutAValueAnswers400NamingIt() throws Exception {
        addEntry("""
                {"templateId": "%s", "rarity": "RED", "bonus": [{"stat": "FIRE_DAMAGE"}]}
                """.formatted(ring.getPublicId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['bonus[0].value']").exists());
    }

    /** A blue copy rolls two lines, so a custom blue bonus of one line or of three is refused. */
    @Test
    void aBonusWithMoreOrFewerLinesThanTheRarityRollsAnswers400() throws Exception {
        String line = "{\"stat\": \"FIRE_DAMAGE\", \"value\": %d}".formatted(FIRE_DAMAGE);
        int blueLines = Rarity.BLUE.bonusCount();
        for (int lines : new int[] {blueLines - 1, blueLines + 1}) {
            addEntry(aBlueEntry(String.join(", ", Collections.nCopies(lines, line))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Wrong Bonus Count"))
                    .andExpect(jsonPath("$.detail").value("A BLUE item carries 2 bonus stats, not " + lines));
        }
        assertThat(standEntryRepository.count()).isZero();
    }

    /** Two lines of the stat, each worth {@link #FIRE_DAMAGE}. */
    private ResultActions addCustomEntry(String stat) throws Exception {
        return addEntry(aBlueEntry("""
                {"stat": "%1$s", "value": %2$d}, {"stat": "%1$s", "value": %2$d}""".formatted(stat, FIRE_DAMAGE)));
    }

    private String aBlueEntry(String bonusLines) {
        return """
                {"templateId": "%s", "rarity": "BLUE", "bonus": [%s]}
                """.formatted(ring.getPublicId(), bonusLines);
    }

    private ResultActions addEntry(String body) throws Exception {
        return mockMvc.perform(post(STAND_URL + "/entries")
                .with(tokenWithRoles(RoleConstants.PLATFORM_ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static JwtRequestPostProcessor tokenWithRoles(String... roles) {
        return jwt()
                .jwt(token -> token.subject(ADMIN.toString())
                        .claim(PublicIdClaim.CLAIM, ADMIN.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
