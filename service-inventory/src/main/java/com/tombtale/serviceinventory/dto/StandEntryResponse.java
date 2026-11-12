package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;

import java.util.Map;
import java.util.UUID;

/**
 * One item on a stand, with what its template shows.
 *
 * @param publicId  the entry's id, which a buy names
 * @param code      its template's code
 * @param name      its template's name
 * @param itemType  its template's type
 * @param iconUrl   the path of its template's icon, or null for a generic one
 * @param baseStats its template's base stats
 * @param rarity    the rarity of every copy
 * @param bonus     the bonus every copy carries, or null when each copy rolls its own
 * @param price     what a copy costs: its template's price times its rarity's multiplier
 */
public record StandEntryResponse(
        UUID publicId,
        String code,
        String name,
        ItemType itemType,
        String iconUrl,
        Map<Stat, Integer> baseStats,
        Rarity rarity,
        Map<Stat, Integer> bonus,
        long price
) {
}
