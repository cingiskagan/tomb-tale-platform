package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.Stat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * An item set as the portal shows it. Every field is named after the entity field behind it (ADR 0017).
 *
 * @param publicId  the set's id in the API
 * @param code      its stable key
 * @param name      the name players see
 * @param bonus     the stats the complete set adds
 * @param version   what an edit sends back
 * @param createdAt when it was created
 * @param updatedAt when it last changed
 */
public record ItemSetResponse(
        UUID publicId,
        String code,
        String name,
        Map<Stat, Integer> bonus,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
}
