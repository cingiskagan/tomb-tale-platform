package com.tombtale.serviceaudit.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One event in {@code events}, under its event id. service-audit can insert one and do nothing else (ADR 0029).
 *
 * @param id           the event id, which a redelivery repeats
 * @param eventType    the event's type
 * @param eventVersion the version of {@code data}
 * @param occurredAt   when the fact committed in the producer
 * @param producer     the service that owns the fact
 * @param data         the fields on the event type's allow-list
 */
@Document("events")
public record StoredEvent(
        @Id UUID id,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        Map<String, Object> data) {
}
