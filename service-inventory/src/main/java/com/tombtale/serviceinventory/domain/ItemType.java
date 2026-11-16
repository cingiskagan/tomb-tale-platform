package com.tombtale.serviceinventory.domain;

/**
 * What kind of item a template is, one per kind the equipment slots take (docs/design/inventory-design.md).
 */
public enum ItemType {

    ONE_HANDED_WEAPON(BonusPool.WEAPON),
    TWO_HANDED_WEAPON(BonusPool.WEAPON),
    SHIELD(BonusPool.ARMOR_PIECE),
    HELMET(BonusPool.ARMOR_PIECE),
    ARMOR(BonusPool.ARMOR_PIECE),
    GLOVES(BonusPool.ARMOR_PIECE),
    BOOTS(BonusPool.ARMOR_PIECE),
    AMULET(BonusPool.JEWELRY),
    RING(BonusPool.JEWELRY);

    /** Every type is equipment, and equipment never stacks. */
    public static final int EQUIPMENT_STACK_SIZE = 1;

    private final BonusPool pool;

    ItemType(BonusPool pool) {
        this.pool = pool;
    }

    /**
     * @return the pool a copy's bonus rolls from
     */
    public BonusPool bonusPool() {
        return pool;
    }
}
