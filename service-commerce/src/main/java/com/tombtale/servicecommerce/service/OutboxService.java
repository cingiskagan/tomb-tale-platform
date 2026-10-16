package com.tombtale.servicecommerce.service;

import com.tombtale.servicecommerce.entity.OutboxEvent;
import com.tombtale.servicecommerce.repository.OutboxEventRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.SenderContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Writes an event into the outbox, inside the caller's transaction and no other.
 * {@code MANDATORY} turns the dual-write mistake into a failure at the call.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ObservationRegistry observationRegistry;

    /**
     * Stores one event for the publisher to send after the commit, with the current trace.
     *
     * @param eventType    the event type, which is also the routing key
     * @param eventVersion the version of the payload shape
     * @param aggregateId  the publicId of the entity the event is about
     * @param data         the data object of the envelope
     * @return the stored row, whose publicId is the eventId
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent append(String eventType, int eventVersion, UUID aggregateId, Object data) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .eventVersion(eventVersion)
                .aggregateId(aggregateId)
                .payload(objectMapper.writeValueAsString(data))
                .build();
        // Micrometer writes a trace only into a carrier, usually a message. Here the row is the carrier,
        // and the setter keeps traceparent, so the publisher can continue this trace later.
        SenderContext<OutboxEvent> trace = new SenderContext<>((row, key, value) -> {
            if (OutboxEvent.TRACEPARENT.equals(key)) {
                row.setTraceparent(value);
            }
        });
        trace.setCarrier(event);
        return Observation.createNotStarted("outbox.append", () -> trace, observationRegistry)
                .lowCardinalityKeyValue("event.type", eventType)
                .observe(() -> outboxEventRepository.save(event));
    }
}
