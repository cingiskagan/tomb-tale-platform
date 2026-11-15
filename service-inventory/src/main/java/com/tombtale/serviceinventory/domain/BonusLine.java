package com.tombtale.serviceinventory.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One roll of an item's bonus. A stat can roll more than once, and each roll is a line of its own.
 *
 * @param stat  the stat it adds to
 * @param value how much it adds
 */
public record BonusLine(Stat stat, int value) {

    /**
     * @param lines a bonus
     * @return each stat's total over the lines, and no entry for a stat no line has
     */
    public static Map<Stat, Integer> totals(List<BonusLine> lines) {
        return lines.stream().collect(Collectors.toMap(
                BonusLine::stat, BonusLine::value, Integer::sum, () -> new EnumMap<>(Stat.class)));
    }
}
