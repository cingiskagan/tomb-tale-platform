package com.tombtale.serviceinventory.exception;

/**
 * The character can't pay the price. Answers 409.
 */
public class NotEnoughCoinsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param coins the character's coins
     * @param price what the item costs
     */
    public NotEnoughCoinsException(long coins, long price) {
        super("The item costs " + price + " coins, and the character has " + coins);
    }
}
