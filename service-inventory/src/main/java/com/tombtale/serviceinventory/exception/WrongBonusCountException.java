package com.tombtale.serviceinventory.exception;

import com.tombtale.serviceinventory.domain.Rarity;

/**
 * A custom bonus has more or fewer lines than its rarity rolls. Answers 400.
 */
public class WrongBonusCountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param rarity the entry's rarity
     * @param lines  how many lines the bonus has
     */
    public WrongBonusCountException(Rarity rarity, int lines) {
        super("A " + rarity + " item carries " + rarity.bonusCount() + " bonus stats, not " + lines);
    }
}
