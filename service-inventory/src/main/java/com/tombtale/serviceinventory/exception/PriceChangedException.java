package com.tombtale.serviceinventory.exception;

/**
 * The price on the stand is no longer the one the player saw. Answers 409.
 */
public class PriceChangedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param price the price on the stand now
     * @param seen  the price the player saw
     */
    public PriceChangedException(long price, long seen) {
        super("The price is now " + price + " coins, not " + seen);
    }
}
