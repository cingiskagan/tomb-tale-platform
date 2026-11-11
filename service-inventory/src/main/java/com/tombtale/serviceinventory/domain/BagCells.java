package com.tombtale.serviceinventory.domain;

import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * A bag cell is one number from 0, so the client chooses how many columns to draw. The bag has no limit yet.
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
}
