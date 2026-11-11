package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BagCellsTest {

    private static final List<Integer> TAKEN = List.of(3, 0, 1);
    private static final List<Integer> FREE = List.of(2, 4, 5);

    @Test
    void theFreeCellsComeLowestFirstAndNeverEnd() {
        PrimitiveIterator.OfInt free = BagCells.free(TAKEN);

        assertThat(Stream.generate(free::next).limit(FREE.size())).containsExactlyElementsOf(FREE);
    }

    @Test
    void anEmptyBagStartsAtZero() {
        assertThat(BagCells.free(List.of()).nextInt()).isZero();
    }
}
