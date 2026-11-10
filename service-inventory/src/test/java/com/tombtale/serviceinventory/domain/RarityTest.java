package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RarityTest {

    /** The design's prices, gray to red. */
    private static final List<Long> PRICE_MULTIPLIERS = List.of(1L, 3L, 9L, 27L, 81L, 243L);
    private static final List<Integer> BONUS_COUNTS = List.of(0, 1, 2, 3, 4, 5);

    @Test
    void eachStepTriplesThePrice() {
        assertThat(Arrays.stream(Rarity.values()).map(Rarity::priceMultiplier)).containsExactlyElementsOf(PRICE_MULTIPLIERS);
    }

    @Test
    void eachStepAddsOneBonus() {
        assertThat(Arrays.stream(Rarity.values()).map(Rarity::bonusCount)).containsExactlyElementsOf(BONUS_COUNTS);
    }
}
