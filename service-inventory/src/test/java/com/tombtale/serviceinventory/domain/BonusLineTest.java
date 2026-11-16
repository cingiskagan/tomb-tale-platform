package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class BonusLineTest {

    private static final int FIRST_FIRE = 3;
    private static final int SECOND_FIRE = 5;
    private static final int ARMOR = 4;

    @Test
    void aStatsLinesAddUp() {
        assertThat(BonusLine.totals(List.of(
                new BonusLine(Stat.FIRE_DAMAGE, FIRST_FIRE),
                new BonusLine(Stat.ARMOR, ARMOR),
                new BonusLine(Stat.FIRE_DAMAGE, SECOND_FIRE))))
                .containsOnly(entry(Stat.FIRE_DAMAGE, FIRST_FIRE + SECOND_FIRE), entry(Stat.ARMOR, ARMOR));
    }

    @Test
    void noLinesHaveNoTotals() {
        assertThat(BonusLine.totals(List.of())).isEmpty();
    }
}
