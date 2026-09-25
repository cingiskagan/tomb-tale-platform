package com.tombtale.serviceplayer.dto.event;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The message on the wire: five envelope fields around the event's data object.
 * docs/design/events.md fixes the shape.
 *
 * @param eventId      the outbox row's publicId, which consumers deduplicate on
 * @param eventType    also the routing key
 * @param eventVersion the version of {@code data}
 * @param occurredAt   when the fact committed, not when it was sent
 * @param producer     the service that owns the fact
 * @param data         the payload stored with the row
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        JsonNode data) {
}
