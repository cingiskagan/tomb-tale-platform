package com.tombtale.serviceinventory.dto.event;

import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Stat;

import java.util.Map;
import java.util.UUID;

/**
 * A template as it stands after a change, sent as {@code item-template.created}, {@code .updated} or {@code .retired}.
 * Each event carries a new version, so a template's events are its history (ADR 0035).
 *
 * @param itemTemplatePublicId the template
 * @param code                 its stable key
 * @param name                 the name players see
 * @param itemType             what kind of item it is
 * @param price                what a gray copy costs, in coins
 * @param baseStats            the stats every copy has before its bonus
 * @param assetKey             the Addressables address of its asset
 * @param iconHash             the hash of its icon, or null for a generic one
 * @param itemSetPublicId      its set, or null
 * @param retired              whether it is out of the game
 * @param version              the version the change made
 * @param updatedBy            the {@code publicId} of the admin who made the change
 */
// PMD reads a record with four public constants as a Data Class, which every event payload is.
@SuppressWarnings("PMD.DataClass")
public record ItemTemplatePayload(
        UUID itemTemplatePublicId,
        String code,
        String name,
        ItemType itemType,
        Long price,
        Map<Stat, Integer> baseStats,
        String assetKey,
        String iconHash,
        UUID itemSetPublicId,
        boolean retired,
        Integer version,
        UUID updatedBy) {

    /** A new template. */
    public static final String CREATED = "item-template.created";

    /** An edit that changed the template. */
    public static final String UPDATED = "item-template.updated";

    /** The template left the game. */
    public static final String RETIRED = "item-template.retired";

    /** The version of this shape, the same for all three types. */
    public static final int EVENT_VERSION = 1;
}
