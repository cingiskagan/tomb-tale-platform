package com.tombtale.serviceaudit.dto.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An event on the wire, of any type. docs/design/events.md fixes the shape.
 *
 * @param eventId      the same on every redelivery of one event
 * @param eventType    also the routing key
 * @param eventVersion the version of {@code data}
 * @param occurredAt   when the fact committed in the producer
 * @param producer     the service that owns the fact
 * @param data         the payload, as the producer sent it
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        Map<String, Object> data) {

    /** An event that lacks a field fails conversion, so it goes to the DLQ with no retry. */
    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(producer, "producer");
        Objects.requireNonNull(data, "data");
    }
}
