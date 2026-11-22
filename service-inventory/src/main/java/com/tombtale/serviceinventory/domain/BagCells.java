package com.tombtale.serviceinventory.domain;

import com.tombtale.serviceinventory.exception.CellOutsideBagException;

import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * A bag cell is one number from 0, so the client chooses how many columns to draw. A cell a client names stays inside
 * {@code inventory.bag-cells}, while grants ignore that size until Tier 2 limits the bag.
 */
public final class BagCells {

    private BagCells() {
    }

    /**
     * @param taken the cells the character's items take
     * @return every cell no item takes, lowest first, without end
     */
    public static PrimitiveIterator.OfInt free(Collection<Integer> taken) {
        Set<Integer> takenCells = Set.copyOf(taken);
        return IntStream.iterate(0, cell -> cell + 1).filter(cell -> !takenCells.contains(cell)).iterator();
    }

    /**
     * @param wanted the cell a client dropped an item on, or null if it named none
     * @param taken  the cells the character's items take
     * @return the wanted cell if no item takes it, otherwise the lowest free cell
     */
    public static int wantedOrLowestFree(Integer wanted, Collection<Integer> taken) {
        return wanted == null || taken.contains(wanted) ? free(taken).nextInt() : wanted;
    }

    /**
     * @param cell  a cell a client names, or null if it named none
     * @param cells how many cells the bag has
     * @throws CellOutsideBagException if the cell is past the bag's last one
     */
    public static void checkInside(Integer cell, int cells) {
        if (cell != null && cell >= cells) {
            throw new CellOutsideBagException(cell, cells);
        }
    }
}
