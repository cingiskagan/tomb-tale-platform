package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Equipping and unequipping from the token to the rows, on a real Postgres. Every request commits, so the slot and
 * cell constraints are checked as they are in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and a matrix of cases needs many methods.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class EquipmentControllerTest extends FixedAuthorTestBase {

    private static final String CHARACTERS_URL = "/api/v1/inventory/characters/";
    private static final UUID OWNER = UUID.randomUUID();
    private static final int CELL = 3;
    private static final String PROBLEM_TITLE = "$.title";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    private CharacterInventory character;

    @BeforeEach
    void aCharacter() {
        character = characterInventoryRepository.save(CharacterInventory.builder().playerId(OWNER).coins(0L).build());
    }

    @AfterEach
    void removeCommittedRows() {
        itemInstanceRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void theReplacedItemTakesTheCellOfTheItemThatReplacesIt() throws Exception {
        ItemInstance worn = anItem(ItemType.HELMET, EquipmentSlot.HEAD, null);
        ItemInstance better = anItem(ItemType.HELMET, null, CELL);

        equip(EquipmentSlot.HEAD, better, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.HEAD.publicId").value(better.getPublicId().toString()))
                .andExpect(jsonPath("$.equipment.HEAD.bagPosition").doesNotExist())
                .andExpect(jsonPath("$.bag[0].publicId").value(worn.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[0].bagPosition").value(CELL));
    }

    @Test
    void aTwoHandedWeaponSendsTheOffHandToTheLowestFreeCell() throws Exception {
        ItemInstance sword = anItem(ItemType.ONE_HANDED_WEAPON, EquipmentSlot.MAIN_HAND, null);
        ItemInstance shield = anItem(ItemType.SHIELD, EquipmentSlot.OFF_HAND, null);
        anItem(ItemType.RING, null, 0);
        ItemInstance axe = anItem(ItemType.TWO_HANDED_WEAPON, null, 1);

        equip(EquipmentSlot.MAIN_HAND, axe, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.MAIN_HAND.publicId").value(axe.getPublicId().toString()))
                .andExpect(jsonPath("$.equipment.OFF_HAND").doesNotExist())
                .andExpect(jsonPath("$.bag[1].publicId").value(sword.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[2].publicId").value(shield.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[2].bagPosition").value(2));
    }

    @Test
    void aShieldSendsATwoHandedWeaponToTheBag() throws Exception {
        ItemInstance axe = anItem(ItemType.TWO_HANDED_WEAPON, EquipmentSlot.MAIN_HAND, null);
        ItemInstance shield = anItem(ItemType.SHIELD, null, CELL);

        equip(EquipmentSlot.OFF_HAND, shield, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.OFF_HAND.publicId").value(shield.getPublicId().toString()))
                .andExpect(jsonPath("$.equipment.MAIN_HAND").doesNotExist())
                .andExpect(jsonPath("$.bag[0].publicId").value(axe.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[0].bagPosition").value(CELL));
    }

    @Test
    void equippingAnItemInItsOwnSlotAgainChangesNothing() throws Exception {
        ItemInstance helmet = anItem(ItemType.HELMET, EquipmentSlot.HEAD, null);

        equip(EquipmentSlot.HEAD, helmet, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.HEAD.publicId").value(helmet.getPublicId().toString()));
    }

    @Test
    void aSlotRefusesAnItemOfAnotherType() throws Exception {
        ItemInstance ring = anItem(ItemType.RING, null, CELL);

        equip(EquipmentSlot.HEAD, ring, ownersToken())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(PROBLEM_TITLE).value("Wrong Slot"));
        assertThat(itemInstanceRepository.findById(ring.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getBagPosition()).isEqualTo(CELL));
    }

    @Test
    void anEquippedItemMustLeaveItsSlotBeforeAnother() throws Exception {
        ItemInstance ring = anItem(ItemType.RING, EquipmentSlot.RING_1, null);

        equip(EquipmentSlot.RING_2, ring, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Item Not In Bag"));
    }

    @Test
    void anotherCharactersItemAnswers404() throws Exception {
        CharacterInventory other = characterInventoryRepository.save(
                CharacterInventory.builder().playerId(OWNER).coins(0L).build());
        ItemInstance theirs = itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(other).template(aTemplate(ItemType.HELMET)).quantity(1).bagPosition(0).build());

        equip(EquipmentSlot.HEAD, theirs, ownersToken())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anItemIsRequired() throws Exception {
        mockMvc.perform(put(slotUrl(EquipmentSlot.HEAD)).with(ownersToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(PROBLEM_TITLE).value("Validation Error"))
                .andExpect(jsonPath("$.errors.itemId").exists());
    }

    @Test
    void unequippingSendsTheItemToTheLowestFreeCell() throws Exception {
        ItemInstance helmet = anItem(ItemType.HELMET, EquipmentSlot.HEAD, null);
        anItem(ItemType.RING, null, 0);
        anItem(ItemType.RING, null, 2);

        mockMvc.perform(delete(slotUrl(EquipmentSlot.HEAD)).with(tokenOf(UUID.randomUUID(), RoleConstants.PLATFORM_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipment.HEAD").doesNotExist())
                .andExpect(jsonPath("$.bag[1].publicId").value(helmet.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[1].bagPosition").value(1));
    }

    @Test
    void unequippingAnEmptySlotChangesNothing() throws Exception {
        mockMvc.perform(delete(slotUrl(EquipmentSlot.HEAD)).with(ownersToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bag").isEmpty());
    }

    @Test
    void anotherPlayersCharacterAnswers403() throws Exception {
        ItemInstance helmet = anItem(ItemType.HELMET, null, 0);
        JwtRequestPostProcessor someoneElse = tokenOf(UUID.randomUUID(), RoleConstants.PLAYER);

        equip(EquipmentSlot.HEAD, helmet, someoneElse).andExpect(status().isForbidden());
        mockMvc.perform(delete(slotUrl(EquipmentSlot.HEAD)).with(someoneElse)).andExpect(status().isForbidden());
    }

    @Test
    void aCallWithoutATokenAnswers401() throws Exception {
        mockMvc.perform(delete(slotUrl(EquipmentSlot.HEAD))).andExpect(status().isUnauthorized());
    }

    private ItemInstance anItem(ItemType type, EquipmentSlot slot, Integer cell) {
        return itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character)
                .template(aTemplate(type))
                .quantity(1)
                .equipmentSlot(slot)
                .bagPosition(cell)
                .build());
    }

    private ItemTemplate aTemplate(ItemType type) {
        return itemTemplateRepository.save(ItemTemplates.aTemplate(type + "_" + UUID.randomUUID()).itemType(type).build());
    }

    private ResultActions equip(EquipmentSlot slot, ItemInstance item, JwtRequestPostProcessor token) throws Exception {
        return mockMvc.perform(put(slotUrl(slot)).with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": \"%s\"}".formatted(item.getPublicId())));
    }

    private String slotUrl(EquipmentSlot slot) {
        return CHARACTERS_URL + character.getPublicId() + "/equipment/" + slot;
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
