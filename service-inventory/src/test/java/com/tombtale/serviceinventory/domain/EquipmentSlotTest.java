package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EquipmentSlotTest {

    @Test
    void theMainHandTakesBothKindsOfWeapon() {
        assertThat(slotsTaking(ItemType.ONE_HANDED_WEAPON)).containsExactly(EquipmentSlot.MAIN_HAND);
        assertThat(slotsTaking(ItemType.TWO_HANDED_WEAPON)).containsExactly(EquipmentSlot.MAIN_HAND);
    }

    @Test
    void aRingFitsEitherRingSlot() {
        assertThat(slotsTaking(ItemType.RING)).containsExactly(EquipmentSlot.RING_1, EquipmentSlot.RING_2);
    }

    @Test
    void everyOtherTypeFitsOneSlot() {
        assertThat(slotsTaking(ItemType.SHIELD)).containsExactly(EquipmentSlot.OFF_HAND);
        assertThat(slotsTaking(ItemType.HELMET)).containsExactly(EquipmentSlot.HEAD);
        assertThat(slotsTaking(ItemType.ARMOR)).containsExactly(EquipmentSlot.CHEST);
        assertThat(slotsTaking(ItemType.GLOVES)).containsExactly(EquipmentSlot.HANDS);
        assertThat(slotsTaking(ItemType.BOOTS)).containsExactly(EquipmentSlot.FEET);
        assertThat(slotsTaking(ItemType.AMULET)).containsExactly(EquipmentSlot.NECK);
    }

    private static List<EquipmentSlot> slotsTaking(ItemType type) {
        return Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.takes(type)).toList();
    }
}
