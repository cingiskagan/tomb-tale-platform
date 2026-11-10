package com.tombtale.serviceinventory.domain;

/**
 * What kind of item a template is, one per kind the equipment slots take (docs/design/inventory-design.md).
 */
public enum ItemType {

    ONE_HANDED_WEAPON,
    TWO_HANDED_WEAPON,
    SHIELD,
    HELMET,
    ARMOR,
    GLOVES,
    BOOTS,
    AMULET,
    RING;

    /** Every type is equipment, and equipment never stacks. */
    public static final int EQUIPMENT_STACK_SIZE = 1;
}
