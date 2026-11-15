package com.tombtale.serviceinventory.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * The stats an item type's bonus rolls from, each with its range (docs/design/inventory-design.md, Bonus stats).
 * The ranges are a first guess, tuned once combat runs.
 */
@SuppressWarnings("PMD.NonSerializableClass") // an enum serializes as its name, never its fields
public enum BonusPool {

    /** Weapons roll damage and critical stats. */
    WEAPON(Map.of(
            Stat.PHYSICAL_DAMAGE, new Range(1, 8),
            Stat.FIRE_DAMAGE, new Range(1, 8),
            Stat.ICE_DAMAGE, new Range(1, 8),
            Stat.DREAD_DAMAGE, new Range(1, 8),
            Stat.DIVINE_DAMAGE, new Range(1, 8),
            Stat.CRIT_CHANCE, new Range(1, 3),
            Stat.CRIT_DAMAGE, new Range(5, 20))),

    /** Armor pieces and shields roll armor, HP, resistances and stamina. */
    ARMOR_PIECE(Map.of(
            Stat.ARMOR, new Range(1, 5),
            Stat.MAX_HP, new Range(5, 30),
            Stat.FIRE_RESISTANCE, new Range(1, 10),
            Stat.ICE_RESISTANCE, new Range(1, 10),
            Stat.DREAD_RESISTANCE, new Range(1, 10),
            Stat.DIVINE_RESISTANCE, new Range(1, 10),
            Stat.MAX_STAMINA, new Range(5, 20),
            Stat.STAMINA_REGENERATION, new Range(1, 3))),

    /** Neck and rings roll any stat, in the range a weapon or an armor piece rolls it. */
    JEWELRY(WEAPON, ARMOR_PIECE);

    // An EnumMap, so a seeded roll picks the same stats on every run.
    private final Map<Stat, Range> rangesByStat;

    BonusPool(Map<Stat, Range> rangesByStat) {
        this.rangesByStat = Collections.unmodifiableMap(new EnumMap<>(rangesByStat));
    }

    BonusPool(BonusPool... merged) {
        this(Arrays.stream(merged)
                .flatMap(pool -> pool.rangesByStat.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * @param rarity the item's rarity, which says how many lines it rolls
     * @param random where the draws come from
     * @return one line per draw, any stat of the pool more than once, each value within its stat's range
     */
    public List<BonusLine> roll(Rarity rarity, RandomGenerator random) {
        List<Stat> stats = List.copyOf(rangesByStat.keySet());
        return random.ints(rarity.bonusCount(), 0, stats.size())
                .mapToObj(stats::get)
                .sorted()
                .map(stat -> new BonusLine(stat, rangesByStat.get(stat).roll(random)))
                .toList();
    }

    /**
     * @return each stat the pool rolls, with its range
     */
    public Map<Stat, Range> ranges() {
        return rangesByStat;
    }

    /**
     * The values one stat rolls, both ends included.
     *
     * @param min the lowest
     * @param max the highest
     */
    public record Range(int min, int max) {

        int roll(RandomGenerator random) {
            return random.nextInt(min, max + 1);
        }
    }
}
