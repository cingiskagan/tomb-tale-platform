package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
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
 * Dealer sells from the token to the rows, on a real Postgres, with the test profile's 30% share.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class DealerSellControllerTest extends FixedAuthorTestBase {

    private static final UUID OWNER = UUID.randomUUID();
    private static final String PROBLEM_TITLE = "$.title";
    private static final String COINS = "$.coins";
    private static final long STARTING_COINS = 1000L;
    private static final long SWORD_PRICE = 155L;
    /** 30% of a blue sword's 1395, rounded down from 418.5. */
    private static final long BLUE_SWORD_PAYOUT = 418L;
    private static final int POTIONS = 3;
    private static final int POTION_STACK = 10;
    private static final long POTION_PRICE = 10L;
    /** 30% of three potions' 30. */
    private static final long POTIONS_PAYOUT = 9L;
    private static final Map<Stat, Integer> ROLLED_BONUS = Map.of(Stat.FIRE_DAMAGE, 4, Stat.CRIT_CHANCE, 1);

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

    private CharacterInventory character;
    private ItemTemplate sword;
    private ItemInstance blueSword;

    /** A character with a blue sword in cell 0. */
    @BeforeEach
    void aCharacterWithABlueSword() {
        character = characterInventoryRepository.save(
                CharacterInventory.builder().playerId(OWNER).coins(STARTING_COINS).build());
        sword = itemTemplateRepository.save(ItemTemplates.aTemplate("SWORD_IRON")
                .itemType(ItemType.ONE_HANDED_WEAPON)
                .price(SWORD_PRICE)
                .build());
        blueSword = itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character)
                .template(sword)
                .rarity(Rarity.BLUE)
                .bonus(ROLLED_BONUS)
                .quantity(1)
                .bagPosition(0)
                .build());
    }

    @AfterEach
    void removeCommittedRows() {
        coinHistoryRepository.deleteAllInBatch();
        itemInstanceRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void aSellPaysAShareOfThePriceRoundedDownAndTakesTheItem() throws Exception {
        UUID tradeId = UUID.randomUUID();

        sell(tradeId, blueSword, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS + BLUE_SWORD_PAYOUT))
                .andExpect(jsonPath("$.bag").isEmpty());

        assertThat(itemInstanceRepository.findById(blueSword.getId())).isEmpty();
        assertThat(coinHistoryRepository.findAll()).singleElement().satisfies(change -> {
            assertThat(change.getAmount()).isEqualTo(BLUE_SWORD_PAYOUT);
            assertThat(change.getReason()).isEqualTo(CoinReason.DEALER_SELL);
            assertThat(change.getReference()).isEqualTo(tradeId);
        });
    }

    @Test
    void aStackSellsWithEveryCopy() throws Exception {
        ItemTemplate potion = itemTemplateRepository.save(
                ItemTemplates.aTemplate("POTION_HEALTH").maxStackSize(POTION_STACK).price(POTION_PRICE).build());
        ItemInstance potions = itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character).template(potion).quantity(POTIONS).bagPosition(1).build());

        sell(UUID.randomUUID(), potions, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS + POTIONS_PAYOUT))
                .andExpect(jsonPath("$.bag.length()").value(1));
    }

    @Test
    void aSellSentTwicePaysOnce() throws Exception {
        UUID tradeId = UUID.randomUUID();

        sell(tradeId, blueSword, ownersToken()).andExpect(status().isOk());
        sell(tradeId, blueSword, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath(COINS).value(STARTING_COINS + BLUE_SWORD_PAYOUT));

        assertThat(coinHistoryRepository.count()).isOne();
    }

    @Test
    void anEquippedItemIsRefused() throws Exception {
        blueSword.setBagPosition(null);
        blueSword.setEquipmentSlot(EquipmentSlot.MAIN_HAND);
        itemInstanceRepository.save(blueSword);

        sell(UUID.randomUUID(), blueSword, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Item Not In Bag"));

        assertNothingChanged();
    }

    @Test
    void aDungeonLockRefusesTheSale() throws Exception {
        character.setDungeonRunId(UUID.randomUUID());
        character.setDungeonLockUntil(Instant.now().plus(Duration.ofMinutes(2)));
        characterInventoryRepository.save(character);

        sell(UUID.randomUUID(), blueSword, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Character In Dungeon"));

        assertNothingChanged();
    }

    /** The player owns both characters, and a sell still never takes one's item for the other's coins. */
    @Test
    void anotherCharactersItemAnswers404() throws Exception {
        CharacterInventory other = characterInventoryRepository.save(
                CharacterInventory.builder().playerId(OWNER).coins(0L).build());
        ItemInstance theirs = itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(other).template(sword).quantity(1).bagPosition(0).build());

        sell(UUID.randomUUID(), theirs, ownersToken())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Not Found"));

        assertThat(itemInstanceRepository.findById(theirs.getId())).isPresent();
        assertNothingChanged();
    }

    @Test
    void anotherPlayersCharacterAnswers403() throws Exception {
        sell(UUID.randomUUID(), blueSword, tokenOf(UUID.randomUUID(), RoleConstants.PLAYER))
                .andExpect(status().isForbidden());

        assertNothingChanged();
    }

    @Test
    void aSellWithoutAnItemIdAnswers400NamingIt() throws Exception {
        mockMvc.perform(post(sellsUrl())
                        .with(ownersToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tradeId\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.itemId").exists());
    }

    private ResultActions sell(UUID tradeId, ItemInstance item, JwtRequestPostProcessor token) throws Exception {
        return mockMvc.perform(post(sellsUrl())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tradeId": "%s", "itemId": "%s"}
                        """.formatted(tradeId, item.getPublicId())));
    }

    private String sellsUrl() {
        return "/api/v1/inventory/characters/" + character.getPublicId() + "/dealer-sells";
    }

    private void assertNothingChanged() {
        assertThat(characterInventoryRepository.findById(character.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getCoins()).isEqualTo(STARTING_COINS));
        assertThat(itemInstanceRepository.findById(blueSword.getId())).isPresent();
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
