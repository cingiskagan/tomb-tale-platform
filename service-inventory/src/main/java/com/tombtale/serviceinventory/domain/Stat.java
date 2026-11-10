package com.tombtale.serviceinventory.domain;

import com.tombtale.serviceinventory.exception.UnknownStatException;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The 15 stats an item can carry, in base stats or in a bonus (docs/design/inventory-design.md, Bonus stats).
 */
public enum Stat {

    PHYSICAL_DAMAGE,
    FIRE_DAMAGE,
    ICE_DAMAGE,
    DREAD_DAMAGE,
    DIVINE_DAMAGE,
    CRIT_CHANCE,
    CRIT_DAMAGE,
    ARMOR,
    MAX_HP,
    FIRE_RESISTANCE,
    ICE_RESISTANCE,
    DREAD_RESISTANCE,
    DIVINE_RESISTANCE,
    MAX_STAMINA,
    STAMINA_REGENERATION;

    /**
     * Reads stats a client sent by name. The match is exact, so {@code fire_damage} is unknown.
     *
     * @param values each stat's name and value
     * @return the same values keyed by stat
     * @throws UnknownStatException if a name is not one of the 15
     */
    public static Map<Stat, Integer> fromNames(Map<String, Integer> values) {
        return values.entrySet().stream().collect(Collectors.toMap(
                entry -> named(entry.getKey()), Map.Entry::getValue, (first, second) -> first, () -> new EnumMap<>(Stat.class)));
    }

    private static Stat named(String name) {
        return Arrays.stream(values())
                .filter(stat -> stat.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new UnknownStatException(name));
    }
}
