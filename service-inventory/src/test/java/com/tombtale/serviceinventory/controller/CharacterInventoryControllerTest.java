package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemIcon;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemSet;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.ItemIconRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemSetRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The inventory read from the token to the rows, on a real Postgres.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and the fixture needs its helpers.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class CharacterInventoryControllerTest extends FixedAuthorTestBase {

    private static final String CHARACTERS_URL = "/api/v1/inventory/characters/";
    private static final UUID OWNER = UUID.randomUUID();
    private static final String ICON_HASH = "a".repeat(64);
    private static final long COINS = 1234L;
    private static final int SWORD_DAMAGE = 12;
    private static final int ROLLED_DAMAGE = 3;
    private static final int ROLLED_FIRE = 4;
    private static final int SET_ARMOR = 10;
    private static final int RING_CELL = 2;
    private static final String SWORD = "$.equipment.MAIN_HAND";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemSetRepository itemSetRepository;

    @Autowired
    private ItemIconRepository itemIconRepository;

    private CharacterInventory character;
    private ItemInstance helmet;

    /** A blue sword and a helmet of the iron set equipped, and a ring in cell 2. */
    @BeforeEach
    void aCharacterWithTheIronSetEquippedAndARingInTheBag() {
        itemIconRepository.save(ItemIcon.builder().hash(ICON_HASH).contentType("image/png").data(new byte[1]).build());
        ItemSet ironSet = itemSetRepository.save(
                ItemSet.builder().code("IRON_SET").name("Iron Set").bonus(Map.of(Stat.ARMOR, SET_ARMOR)).build());
        character = characterInventoryRepository.save(CharacterInventory.builder().playerId(OWNER).coins(COINS).build());

        ItemTemplate sword = itemTemplateRepository.save(aTemplate("SWORD_IRON", ItemType.ONE_HANDED_WEAPON, ironSet)
                .baseStats(Map.of(Stat.PHYSICAL_DAMAGE, SWORD_DAMAGE))
                .iconHash(ICON_HASH)
                .build());
        itemInstanceRepository.save(anItem(sword)
                .rarity(Rarity.BLUE)
                .bonus(Map.of(Stat.PHYSICAL_DAMAGE, ROLLED_DAMAGE, Stat.FIRE_DAMAGE, ROLLED_FIRE))
                .equipmentSlot(EquipmentSlot.MAIN_HAND)
                .build());
        helmet = itemInstanceRepository.save(
                anItem(itemTemplateRepository.save(aTemplate("HELMET_IRON", ItemType.HELMET, ironSet).build()))
                        .equipmentSlot(EquipmentSlot.HEAD)
                        .build());
        itemInstanceRepository.save(
                anItem(itemTemplateRepository.save(aTemplate("RING_GOLD", ItemType.RING, null).build()))
                        .bagPosition(RING_CELL)
                        .build());
    }

    @AfterEach
    void removeCommittedRows() {
        itemInstanceRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
        itemSetRepository.deleteAllInBatch();
        itemIconRepository.deleteAllInBatch();
    }

    @Test
    void anAdminReadsTheEquipmentBySlotAndTheBagByCell() throws Exception {
        read(character.getPublicId(), tokenOf(UUID.randomUUID(), RoleConstants.PLATFORM_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterId").value(character.getPublicId().toString()))
                .andExpect(jsonPath("$.coins").value(COINS))
                .andExpect(jsonPath(SWORD + ".code").value("SWORD_IRON"))
                .andExpect(jsonPath(SWORD + ".rarity").value("BLUE"))
                .andExpect(jsonPath(SWORD + ".iconUrl").value("/api/v1/inventory/icons/" + ICON_HASH))
                .andExpect(jsonPath(SWORD + ".bonus.FIRE_DAMAGE").value(ROLLED_FIRE))
                .andExpect(jsonPath(SWORD + ".finalStats.PHYSICAL_DAMAGE").value(SWORD_DAMAGE + ROLLED_DAMAGE))
                .andExpect(jsonPath(SWORD + ".finalStats.FIRE_DAMAGE").value(ROLLED_FIRE))
                .andExpect(jsonPath("$.equipment.HEAD.code").value("HELMET_IRON"))
                .andExpect(jsonPath("$.equipment.OFF_HAND").doesNotExist())
                .andExpect(jsonPath("$.bag[0].code").value("RING_GOLD"))
                .andExpect(jsonPath("$.bag[0].bagPosition").value(RING_CELL))
                .andExpect(jsonPath("$.setBonuses[0].code").value("IRON_SET"))
                .andExpect(jsonPath("$.setBonuses[0].bonus.ARMOR").value(SET_ARMOR));
    }

    @Test
    void aSetWithAPieceInTheBagGivesNoBonus() throws Exception {
        helmet.setEquipmentSlot(null);
        helmet.setBagPosition(0);
        itemInstanceRepository.save(helmet);

        read(character.getPublicId(), tokenOf(OWNER, RoleConstants.PLAYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bag[0].code").value("HELMET_IRON"))
                .andExpect(jsonPath("$.setBonuses").isEmpty());
    }

    @Test
    void anotherPlayersCharacterAnswers403() throws Exception {
        read(character.getPublicId(), tokenOf(UUID.randomUUID(), RoleConstants.PLAYER, RoleConstants.GAME_MASTER))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownCharacterAnswers404() throws Exception {
        read(UUID.randomUUID(), tokenOf(OWNER, RoleConstants.PLAYER))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void aCallWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(get(CHARACTERS_URL + character.getPublicId())).andExpect(status().isUnauthorized());
    }

    private static ItemTemplate.ItemTemplateBuilder<?, ?> aTemplate(String code, ItemType type, ItemSet set) {
        return ItemTemplates.aTemplate(code).itemType(type).itemSet(set);
    }

    private ItemInstance.ItemInstanceBuilder<?, ?> anItem(ItemTemplate template) {
        return ItemInstance.builder().characterInventory(character).template(template).quantity(1);
    }

    private ResultActions read(UUID characterId, JwtRequestPostProcessor token) throws Exception {
        return mockMvc.perform(get(CHARACTERS_URL + characterId).with(token));
    }

    private static JwtRequestPostProcessor tokenOf(UUID player, String... roles) {
        return jwt()
                .jwt(token -> token.subject(player.toString())
                        .claim(PublicIdClaim.CLAIM, player.toString())
                        .claim(RoleClaimConverter.ROLES_CLAIM, List.of(roles)))
                .authorities(new RoleClaimConverter());
    }
}
