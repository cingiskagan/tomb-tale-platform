package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.BonusPool;
import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.entity.StandEntry;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import com.tombtale.serviceinventory.repository.DealerRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dealer buys from the token to the rows, on a real Postgres. Every request commits, as it does in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class DealerBuyControllerTest extends FixedAuthorTestBase {

    private static final UUID OWNER = UUID.randomUUID();
    private static final String PROBLEM_TITLE = "$.title";
    private static final String COINS = "$.coins";
    private static final long STARTING_COINS = 100_000L;
    private static final long SWORD_PRICE = 150L;
    /** Blue is 9 times gray. */
    private static final long BLUE_SWORD_PRICE = 1350L;
    private static final Map<Stat, Integer> CUSTOM_BONUS = Map.of(Stat.FIRE_DAMAGE, 40, Stat.CRIT_CHANCE, 2);
    private static final Duration LOCK_TIME = Duration.ofMinutes(2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private CoinHistoryRepository coinHistoryRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private DealerRepository dealerRepository;

    @Autowired
    private StandEntryRepository standEntryRepository;

    private CharacterInventory character;
    private ItemTemplate sword;
    private StandEntry blueSword;

    /** A character with a ring in cell 0, and a blacksmith who sells a blue sword. */
    @BeforeEach
    void aCharacterAndABlacksmith() {
        character = characterInventoryRepository.save(
                CharacterInventory.builder().playerId(OWNER).coins(STARTING_COINS).build());
        sword = itemTemplateRepository.save(ItemTemplates.aTemplate("SWORD_IRON")
                .itemType(ItemType.ONE_HANDED_WEAPON)
                .price(SWORD_PRICE)
                .build());
        ItemTemplate ring = itemTemplateRepository.save(ItemTemplates.aTemplate("RING_IRON").build());
        itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character).template(ring).quantity(1).bagPosition(0).build());
        blueSword = anEntry(sword, Rarity.BLUE, null);
    }

    @AfterEach
    void removeCommittedRows() {
        coinHistoryRepository.deleteAllInBatch();
        itemInstanceRepository.deleteAllInBatch();
        standEntryRepository.deleteAllInBatch();
        dealerRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void aBuyChargesThePriceAndPutsARolledCopyInTheLowestFreeCell() throws Exception {
        UUID tradeId = UUID.randomUUID();

        buy(tradeId, blueSword, BLUE_SWORD_PRICE, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS - BLUE_SWORD_PRICE))
                .andExpect(jsonPath("$.bag[1].code").value("SWORD_IRON"))
                .andExpect(jsonPath("$.bag[1].rarity").value("BLUE"))
                .andExpect(jsonPath("$.bag[1].bagPosition").value(1));

        Map<Stat, BonusPool.Range> weaponRanges = BonusPool.WEAPON.ranges();
        assertThat(boughtSwords()).singleElement().satisfies(bought -> {
            assertThat(bought.getBonus()).hasSize(Rarity.BLUE.bonusCount());
            bought.getBonus().forEach((stat, value) -> assertThat(value)
                    .isBetween(weaponRanges.get(stat).min(), weaponRanges.get(stat).max()));
        });
        assertThat(coinHistoryRepository.findAll()).singleElement().satisfies(change -> {
            assertThat(change.getAmount()).isEqualTo(-BLUE_SWORD_PRICE);
            assertThat(change.getReason()).isEqualTo(CoinReason.DEALER_BUY);
            assertThat(change.getReference()).isEqualTo(tradeId);
        });
    }

    /** A red copy would roll five stats, so two stats show the bonus was copied, not rolled. */
    @Test
    void aCustomEntryCopiesItsBonusExactly() throws Exception {
        StandEntry custom = anEntry(sword, Rarity.RED, CUSTOM_BONUS);

        buy(UUID.randomUUID(), custom, SWORD_PRICE * Rarity.RED.priceMultiplier(), ownersToken())
                .andExpect(status().isOk());

        assertThat(boughtSwords()).singleElement()
                .satisfies(bought -> assertThat(bought.getBonus()).isEqualTo(CUSTOM_BONUS));
    }

    @Test
    void aBuySentTwiceChargesOnce() throws Exception {
        UUID tradeId = UUID.randomUUID();

        buy(tradeId, blueSword, BLUE_SWORD_PRICE, ownersToken()).andExpect(status().isOk());
        buy(tradeId, blueSword, BLUE_SWORD_PRICE, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS - BLUE_SWORD_PRICE))
                .andExpect(jsonPath("$.bag.length()").value(2));

        assertThat(boughtSwords()).hasSize(1);
        assertThat(coinHistoryRepository.count()).isOne();
    }

    @Test
    void aChangedPriceAnswers409AndChargesNothing() throws Exception {
        buy(UUID.randomUUID(), blueSword, SWORD_PRICE, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Price Changed"))
                .andExpect(jsonPath("$.detail").value("The price is now 1350 coins, not 150"));

        assertNothingChanged();
    }

    @Test
    void tooFewCoinsAnswers409() throws Exception {
        character.setCoins(BLUE_SWORD_PRICE - 1);
        characterInventoryRepository.save(character);

        buy(UUID.randomUUID(), blueSword, BLUE_SWORD_PRICE, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Not Enough Coins"));

        assertThat(boughtSwords()).isEmpty();
        assertThat(coinHistoryRepository.count()).isZero();
    }

    @Test
    void aDungeonLockRefusesTheBuyUntilItEnds() throws Exception {
        character.setDungeonRunId(UUID.randomUUID());
        character.setDungeonLockUntil(Instant.now().plus(LOCK_TIME));
        character = characterInventoryRepository.save(character);

        buy(UUID.randomUUID(), blueSword, BLUE_SWORD_PRICE, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Character In Dungeon"));
        assertNothingChanged();

        // The game server stopped renewing it, as it does when it crashes.
        character.setDungeonLockUntil(Instant.now().minus(LOCK_TIME));
        characterInventoryRepository.save(character);
        buy(UUID.randomUUID(), blueSword, BLUE_SWORD_PRICE, ownersToken()).andExpect(status().isOk());
    }

    @Test
    void anEntryNoStandShowsAnswers404() throws Exception {
        Dealer alchemist = dealerRepository.save(
                Dealer.builder().code("TOWN_ALCHEMIST").name("Alchemist").retired(true).build());
        StandEntry retiredDealers = standEntryRepository.save(StandEntry.builder()
                .dealer(alchemist).template(sword).rarity(Rarity.GRAY).build());
        ItemTemplate axe = itemTemplateRepository.save(ItemTemplates.aTemplate("AXE_IRON").retired(true).build());
        StandEntry retiredTemplates = anEntry(axe, Rarity.GRAY, null);

        for (StandEntry gone : List.of(retiredDealers, retiredTemplates)) {
            buy(UUID.randomUUID(), gone, 0L, ownersToken())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Stand entry not found with id: " + gone.getPublicId()));
        }
        assertNothingChanged();
    }

    @Test
    void anAdminBuysForAnyCharacterAndAnotherPlayerIsRefused() throws Exception {
        buy(UUID.randomUUID(), blueSword, BLUE_SWORD_PRICE, tokenOf(UUID.randomUUID(), RoleConstants.PLAYER))
                .andExpect(status().isForbidden());
        assertNothingChanged();

        buy(UUID.randomUUID(), blueSword, BLUE_SWORD_PRICE, tokenOf(UUID.randomUUID(), RoleConstants.PLATFORM_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS - BLUE_SWORD_PRICE));
    }

    @Test
    void aBuyWithoutATradeIdAnswers400NamingIt() throws Exception {
        mockMvc.perform(post(buysUrl())
                        .with(ownersToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryId": "%s", "price": %d}
                                """.formatted(blueSword.getPublicId(), BLUE_SWORD_PRICE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.tradeId").exists());
    }

    @Test
    void aBuyWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(post(buysUrl()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private StandEntry anEntry(ItemTemplate template, Rarity rarity, Map<Stat, Integer> bonus) {
        Dealer blacksmith = dealerRepository.findByCode("TOWN_BLACKSMITH").orElseGet(() ->
                dealerRepository.save(Dealer.builder().code("TOWN_BLACKSMITH").name("Blacksmith").build()));
        return standEntryRepository.save(StandEntry.builder()
                .dealer(blacksmith).template(template).rarity(rarity).bonus(bonus).build());
    }

    private ResultActions buy(UUID tradeId, StandEntry entry, long price, JwtRequestPostProcessor token)
            throws Exception {
        return mockMvc.perform(post(buysUrl())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tradeId": "%s", "entryId": "%s", "price": %d}
                        """.formatted(tradeId, entry.getPublicId(), price)));
    }

    private String buysUrl() {
        return "/api/v1/inventory/characters/" + character.getPublicId() + "/dealer-buys";
    }

    private List<ItemInstance> boughtSwords() {
        return itemInstanceRepository.findAll().stream()
                .filter(item -> item.getTemplate().getId().equals(sword.getId()))
                .toList();
    }

    private void assertNothingChanged() {
        assertThat(characterInventoryRepository.findById(character.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getCoins()).isEqualTo(STARTING_COINS));
        assertThat(boughtSwords()).isEmpty();
        assertThat(coinHistoryRepository.count()).isZero();
    }

    private static JwtRequestPostProcessor ownersToken() {
        return tokenOf(OWNER, RoleConstants.PLAYER);
    }

    private static JwtRequestPostProcessor tokenOf(UUID player, String... roles) {
        return jwt()
                .jwt(token -> token.subject(player.toString())
                        .claim(PublicIdClaim.CLAIM, player.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
