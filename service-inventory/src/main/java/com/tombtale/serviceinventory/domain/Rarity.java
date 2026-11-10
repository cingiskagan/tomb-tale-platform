package com.tombtale.serviceinventory.domain;

/**
 * An item's rarity, lowest first. Each step up triples the price and adds one bonus stat
 * (docs/design/inventory-design.md, Items and Prices).
 */
public enum Rarity {

    GRAY,
    GREEN,
    BLUE,
    PURPLE,
    ORANGE,
    RED;

    private static final long PRICE_STEP = 3;

    /**
     * @return what a copy of this rarity costs, as a multiple of the template's price: 1 for gray, 243 for red
     */
    public long priceMultiplier() {
        long multiplier = 1;
        for (int step = 0; step < ordinal(); step++) {
            multiplier *= PRICE_STEP;
        }
        return multiplier;
    }

    /**
     * @return how many bonus stats a copy rolls: none for gray, 5 for red
     */
    public int bonusCount() {
        return ordinal();
    }
}
