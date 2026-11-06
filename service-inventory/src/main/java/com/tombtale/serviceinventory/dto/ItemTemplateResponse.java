package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Stat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A template as the portal shows it. Every field is named after the entity field behind it (ADR 0017).
 *
 * @param publicId     the template's id in the API
 * @param code         its stable key
 * @param name         the name players see
 * @param itemType     what kind of item it is
 * @param maxStackSize how many copies one stack holds
 * @param price        what a gray copy costs, in coins
 * @param baseStats    the stats every copy has before its bonus
 * @param assetKey     the Addressables address of its asset
 * @param iconHash     the hash of its icon, or null for a generic one
 * @param retired      whether it is out of the game
 * @param version      what an edit sends back
 * @param createdAt    when it was created
 * @param updatedAt    when it last changed
 */
public record ItemTemplateResponse(
        UUID publicId,
        String code,
        String name,
        ItemType itemType,
        Integer maxStackSize,
        Long price,
        Map<Stat, Integer> baseStats,
        String assetKey,
        String iconHash,
        boolean retired,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
}
