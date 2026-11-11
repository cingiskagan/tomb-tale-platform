package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class StatTest {

    private static final int BASE_DAMAGE = 12;
    private static final int BONUS_DAMAGE = 3;
    private static final int BONUS_FIRE = 4;

    @Test
    void addsUpEachStatAndKeepsTheOnesOnlyOnePartHas() {
        Map<Stat, Integer> total = Stat.sum(List.of(
                Map.of(Stat.PHYSICAL_DAMAGE, BASE_DAMAGE),
                Map.of(Stat.PHYSICAL_DAMAGE, BONUS_DAMAGE, Stat.FIRE_DAMAGE, BONUS_FIRE)));

        assertThat(total).containsOnly(
                entry(Stat.PHYSICAL_DAMAGE, BASE_DAMAGE + BONUS_DAMAGE), entry(Stat.FIRE_DAMAGE, BONUS_FIRE));
    }
}
