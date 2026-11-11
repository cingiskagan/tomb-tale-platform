package com.tombtale.serviceinventory.exception;

/**
 * A move names a cell past the bag's last one. Answers 400.
 */
public class CellOutsideBagException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param cell  the cell the client named
     * @param cells how many cells the bag has
     */
    public CellOutsideBagException(int cell, int cells) {
        super("Cell " + cell + " is outside the bag's " + cells + " cells");
    }
}
