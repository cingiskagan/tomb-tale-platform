package com.tombtale.serviceinventory.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A dealer as the portal shows it. Every field is named after the entity field behind it (ADR 0017).
 *
 * @param publicId  the dealer's id in the API
 * @param code      its stable key, which its stand's path names
 * @param name      the name players see
 * @param retired   whether its stand shows nothing
 * @param version   what an edit sends back
 * @param createdAt when it was created
 * @param updatedAt when it last changed
 */
public record DealerResponse(
        UUID publicId,
        String code,
        String name,
        boolean retired,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
}
