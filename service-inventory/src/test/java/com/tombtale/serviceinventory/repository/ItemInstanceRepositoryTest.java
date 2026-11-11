package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.config.JpaConfig;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.support.FixedAuthorTestBase;
import com.tombtale.serviceinventory.support.ItemTemplates;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The slot and cell constraints. A test transaction never commits, so {@code SET CONSTRAINTS ALL IMMEDIATE} runs
 * the checks a commit would.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ItemInstanceRepositoryTest extends FixedAuthorTestBase {

    @Autowired
    private TestEntityManager entityManager;

    private CharacterInventory character;
    private ItemTemplate ring;

    @BeforeEach
    void aCharacterAndARing() {
        character = entityManager.persist(CharacterInventory.builder().playerId(UUID.randomUUID()).coins(0L).build());
        ring = entityManager.persist(ItemTemplates.aTemplate("RING_IRON").build());
    }

    @Test
    void anItemIsInASlotOrInACellButNotBoth() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(anItem(EquipmentSlot.RING_1, 0)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_item_instances_slot_or_cell");
    }

    @Test
    void anItemIsInASlotOrInACellButNotNeither() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(anItem(null, null)))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_item_instances_slot_or_cell");
    }

    @Test
    void aSwapSharesACellHalfwayAndPassesTheCheck() {
        ItemInstance first = entityManager.persist(anItem(null, 0));
        ItemInstance second = entityManager.persistAndFlush(anItem(null, 1));

        first.setBagPosition(1);
        entityManager.flush();
        second.setBagPosition(0);
        entityManager.flush();

        assertThatCode(this::checkAsTheCommitWould).doesNotThrowAnyException();
    }

    @Test
    void twoItemsLeftInOneSlotFailTheCheck() {
        entityManager.persist(anItem(EquipmentSlot.RING_1, null));
        entityManager.persistAndFlush(anItem(EquipmentSlot.RING_1, null));

        assertThatThrownBy(this::checkAsTheCommitWould)
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uq_item_instances_character_slot");
    }

    private ItemInstance anItem(EquipmentSlot slot, Integer cell) {
        return ItemInstance.builder()
                .characterInventory(character)
                .template(ring)
                .quantity(1)
                .equipmentSlot(slot)
                .bagPosition(cell)
                .build();
    }

    private void checkAsTheCommitWould() {
        entityManager.getEntityManager().createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
    }
}
