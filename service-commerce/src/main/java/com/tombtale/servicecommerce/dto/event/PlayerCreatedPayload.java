package com.tombtale.servicecommerce.dto.event;

import java.util.UUID;

/**
 * The fields of {@code player.created} that commerce reads.
 * The converter ignores the rest, so a field the producer adds breaks nothing here.
 *
 * @param playerPublicId the new player
 */
public record PlayerCreatedPayload(UUID playerPublicId) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "player.created";
}
