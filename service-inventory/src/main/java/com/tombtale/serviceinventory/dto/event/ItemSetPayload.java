package com.tombtale.serviceinventory.dto.event;

import com.tombtale.serviceinventory.domain.Stat;

import java.util.Map;
import java.util.UUID;

/**
 * A set as it stands after a change, sent as {@code item-set.created} or {@code .updated}.
 * Each event carries a new version, so a set's events are its history (ADR 0035).
 *
 * @param itemSetPublicId the set
 * @param code            its stable key
 * @param name            the name players see
 * @param bonus           the stats the complete set adds
 * @param version         the version the change made
 * @param updatedBy       the {@code publicId} of the admin who made the change
 */
public record ItemSetPayload(
        UUID itemSetPublicId,
        String code,
        String name,
        Map<Stat, Integer> bonus,
        Integer version,
        UUID updatedBy) {

    /** A new set. */
    public static final String CREATED = "item-set.created";

    /** An edit that changed the set. */
    public static final String UPDATED = "item-set.updated";

    /** The version of this shape, the same for both types. */
    public static final int EVENT_VERSION = 1;
}
