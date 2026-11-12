package com.tombtale.serviceinventory.domain;

/**
 * The share of an item's price that every dealer pays for it, rounded down to whole coins. It is at most 100%,
 * so no route between dealers makes coins.
 *
 * @param percent the share, from 0 to 100
 */
public record SellShare(int percent) {

    private static final int WHOLE = 100;

    /** @throws IllegalArgumentException if the share is below 0% or above 100% */
    public SellShare {
        if (percent < 0 || percent > WHOLE) {
            throw new IllegalArgumentException("A sell share is 0 to 100 percent, not " + percent);
        }
    }

    /**
     * @param price what the item costs at a dealer
     * @return what a dealer pays for it
     */
    public long payoutFor(long price) {
        return price * percent / WHOLE;
    }
}
