package com.tombtale.serviceinventory.domain;

import com.tombtale.serviceinventory.exception.CellOutsideBagException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BagCellsTest {

    private static final List<Integer> TAKEN = List.of(3, 0, 1);
    private static final List<Integer> FREE = List.of(2, 4, 5);
    private static final int BAG_CELLS = 40;

    @Test
    void theFreeCellsComeLowestFirstAndNeverEnd() {
        PrimitiveIterator.OfInt free = BagCells.free(TAKEN);

        assertThat(Stream.generate(free::next).limit(FREE.size())).containsExactlyElementsOf(FREE);
    }

    @Test
    void anEmptyBagStartsAtZero() {
        assertThat(BagCells.free(List.of()).nextInt()).isZero();
    }

    /** A free cell the client names is the one the item takes. */
    @Test
    void aFreeWantedCellIsTheOneTaken() {
        assertThat(BagCells.wantedOrLowestFree(FREE.get(1), TAKEN)).isEqualTo(FREE.get(1));
    }

    /** A taken cell, or none at all, falls back to the lowest free cell. */
    @Test
    void aTakenOrMissingCellFallsBackToTheLowestFree() {
        assertThat(BagCells.wantedOrLowestFree(TAKEN.get(0), TAKEN)).isEqualTo(FREE.get(0));
        assertThat(BagCells.wantedOrLowestFree(null, TAKEN)).isEqualTo(FREE.get(0));
    }

    /** The last cell and no cell pass, and the cell after the last is refused. */
    @Test
    void onlyACellPastTheLastIsOutsideTheBag() {
        assertThatNoException().isThrownBy(() -> BagCells.checkInside(BAG_CELLS - 1, BAG_CELLS));
        assertThatNoException().isThrownBy(() -> BagCells.checkInside(null, BAG_CELLS));
        assertThatThrownBy(() -> BagCells.checkInside(BAG_CELLS, BAG_CELLS))
                .isInstanceOf(CellOutsideBagException.class)
                .hasMessage("Cell 40 is outside the bag's 40 cells");
    }
}
