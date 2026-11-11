package com.tombtale.serviceinventory.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class BonusPoolTest {

    /** Enough red rolls that every jewelry stat reaches both ends of its range. */
    private static final int ROLLS = 2000;
    private static final long SEED = 20_261_112L;

    private final RandomGenerator random = new SplittableRandom(SEED);

    @Test
    void itemTypesShareAPoolAsTheDesignGroupsThem() {
        assertThat(Arrays.stream(ItemType.values()).collect(Collectors.groupingBy(ItemType::bonusPool)))
                .containsOnly(
                        entry(BonusPool.WEAPON, List.of(ItemType.ONE_HANDED_WEAPON, ItemType.TWO_HANDED_WEAPON)),
                        entry(BonusPool.ARMOR_PIECE, List.of(
                                ItemType.SHIELD, ItemType.HELMET, ItemType.ARMOR, ItemType.GLOVES, ItemType.BOOTS)),
                        entry(BonusPool.JEWELRY, List.of(ItemType.AMULET, ItemType.RING)));
    }

    @Test
    void weaponsRollOffenseArmorPiecesTheRestAndJewelryAnyStat() {
        Set<Stat> offense = EnumSet.range(Stat.PHYSICAL_DAMAGE, Stat.CRIT_DAMAGE);

        assertThat(BonusPool.WEAPON.ranges().keySet()).isEqualTo(offense);
        assertThat(BonusPool.ARMOR_PIECE.ranges().keySet()).isEqualTo(EnumSet.complementOf(EnumSet.copyOf(offense)));
        assertThat(BonusPool.JEWELRY.ranges().keySet()).isEqualTo(EnumSet.allOf(Stat.class));
    }

    @ParameterizedTest
    @EnumSource(Rarity.class)
    void anItemRollsAsManyDifferentStatsAsItsRarityAdds(Rarity rarity) {
        for (BonusPool pool : BonusPool.values()) {
            for (int roll = 0; roll < ROLLS; roll++) {
                Map<Stat, Integer> bonus = pool.roll(rarity, random);
                assertThat(bonus).as(pool.name()).hasSize(rarity.bonusCount());
                assertThat(pool.ranges().keySet()).containsAll(bonus.keySet());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(BonusPool.class)
    void eachValueFallsWithinItsRangeAndReachesBothEnds(BonusPool pool) {
        Map<Stat, IntSummaryStatistics> rolled = Stream.generate(() -> pool.roll(Rarity.RED, random))
                .limit(ROLLS)
                .flatMap(bonus -> bonus.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summarizingInt(Map.Entry::getValue)));

        assertThat(rolled.keySet()).isEqualTo(pool.ranges().keySet());
        pool.ranges().forEach((stat, range) -> {
            assertThat(rolled.get(stat).getMin()).as("%s min", stat).isEqualTo(range.min());
            assertThat(rolled.get(stat).getMax()).as("%s max", stat).isEqualTo(range.max());
        });
    }
}
