package com.tombtale.serviceinventory.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * A character's nine equipment slots, each with the item types it takes (docs/design/inventory-design.md, Equipment).
 */
public enum EquipmentSlot {

    MAIN_HAND(ItemType.ONE_HANDED_WEAPON, ItemType.TWO_HANDED_WEAPON),
    OFF_HAND(ItemType.SHIELD),
    HEAD(ItemType.HELMET),
    CHEST(ItemType.ARMOR),
    HANDS(ItemType.GLOVES),
    FEET(ItemType.BOOTS),
    NECK(ItemType.AMULET),
    RING_1(ItemType.RING),
    RING_2(ItemType.RING);

    private final Set<ItemType> types;

    EquipmentSlot(ItemType first, ItemType... others) {
        this.types = EnumSet.of(first, others);
    }

    /**
     * @param type an item's type
     * @return whether an item of that type fits this slot
     */
    public boolean takes(ItemType type) {
        return types.contains(type);
    }
}
