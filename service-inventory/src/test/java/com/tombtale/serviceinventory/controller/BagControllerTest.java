package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.security.RoleClaimConverter;
import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
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
import org.springframework.beans.factory.annotation.Value;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bag moves from the token to the rows, on a real Postgres. Every request commits, so a swap meets the cell
 * constraint as it does in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// MockMvc's andExpect is the assertion, which PMD does not see, and the fixture needs its helpers.
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods", "PMD.TooManyStaticImports"})
class BagControllerTest extends FixedAuthorTestBase {

    private static final UUID OWNER = UUID.randomUUID();
    private static final int EMPTY_CELL = 7;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Value("${inventory.bag-cells}")
    private int bagCells;

    private CharacterInventory character;
    private ItemTemplate ring;

    @BeforeEach
    void aCharacterAndARing() {
        character = characterInventoryRepository.save(CharacterInventory.builder().playerId(OWNER).coins(0L).build());
        ring = itemTemplateRepository.save(ItemTemplates.aTemplate("RING_IRON").build());
    }

    @AfterEach
    void removeCommittedRows() {
        itemInstanceRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void anItemMovesToAnEmptyCell() throws Exception {
        ItemInstance item = aRing(null, 0);

        move(item, EMPTY_CELL, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bag[0].publicId").value(item.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[0].bagPosition").value(EMPTY_CELL));
    }

    /** Both rows hold cell 1 for a moment, which only a check at the commit allows. */
    @Test
    void aMoveToATakenCellSwapsTheTwoItems() throws Exception {
        ItemInstance first = aRing(null, 0);
        ItemInstance second = aRing(null, 1);

        move(first, 1, ownersToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bag[0].publicId").value(second.getPublicId().toString()))
                .andExpect(jsonPath("$.bag[1].publicId").value(first.getPublicId().toString()));

        assertThat(itemInstanceRepository.findById(first.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getBagPosition()).isOne());
        assertThat(itemInstanceRepository.findById(second.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getBagPosition()).isZero());
    }

    @Test
    void anEquippedItemIsUnequippedInsteadOfMoved() throws Exception {
        ItemInstance item = aRing(EquipmentSlot.RING_1, null);

        move(item, 0, ownersToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Item Not In Bag"));
    }

    @Test
    void aCellBelowZeroAnswers400() throws Exception {
        move(aRing(null, 0), -1, ownersToken()).andExpect(status().isBadRequest());
    }

    @Test
    void aMoveStaysInsideTheBag() throws Exception {
        ItemInstance item = aRing(null, 0);

        move(item, bagCells, ownersToken())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Cell Outside Bag"));
        move(item, bagCells - 1, ownersToken()).andExpect(status().isOk());
    }

    /** The player owns both characters, and a move still never takes an item from one to the other. */
    @Test
    void anotherCharactersItemAnswers404() throws Exception {
        CharacterInventory other = characterInventoryRepository.save(
                CharacterInventory.builder().playerId(OWNER).coins(0L).build());
        ItemInstance theirs = itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(other).template(ring).quantity(1).bagPosition(0).build());

        move(theirs, 1, ownersToken())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
        assertThat(itemInstanceRepository.findById(theirs.getId()))
                .hasValueSatisfying(stored -> assertThat(stored.getBagPosition()).isZero());
    }

    @Test
    void anotherPlayersCharacterAnswers403() throws Exception {
        move(aRing(null, 0), 1, tokenOf(UUID.randomUUID(), RoleConstants.PLAYER)).andExpect(status().isForbidden());
    }

    private ItemInstance aRing(EquipmentSlot slot, Integer cell) {
        return itemInstanceRepository.save(ItemInstance.builder()
                .characterInventory(character).template(ring).quantity(1).equipmentSlot(slot).bagPosition(cell).build());
    }

    private ResultActions move(ItemInstance item, int cell, JwtRequestPostProcessor token) throws Exception {
        return mockMvc.perform(put("/api/v1/inventory/characters/" + character.getPublicId() + "/bag/" + cell)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemId\": \"%s\"}".formatted(item.getPublicId())));
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
