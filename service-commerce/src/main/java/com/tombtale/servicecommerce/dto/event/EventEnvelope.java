package com.tombtale.servicecommerce.dto.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An event as it arrives from the broker. docs/design/events.md fixes the shape.
 *
 * @param eventId      the same on every redelivery of one event
 * @param eventType    also the routing key
 * @param eventVersion the version of {@code data}
 * @param occurredAt   when the fact committed in the producer
 * @param producer     the service that owns the fact
 * @param data         the payload of this event type
 * @param <T>          the payload type
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        T data) {

    /** An event with no payload fails conversion, so it goes to the DLQ with no retry. */
    public EventEnvelope {
        Objects.requireNonNull(data, "data");
    }
}
