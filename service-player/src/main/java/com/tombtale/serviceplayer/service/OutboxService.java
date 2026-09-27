package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
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

    /**
     * Stores one event for the publisher to send after the commit.
     *
     * @param eventType    the event type, which is also the routing key
     * @param eventVersion the version of the payload shape
     * @param aggregateId  the publicId of the entity the event is about
     * @param data         the data object of the envelope
     * @return the stored row, whose publicId is the eventId
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent append(String eventType, int eventVersion, UUID aggregateId, Object data) {
        return outboxEventRepository.save(OutboxEvent.builder()
                .eventType(eventType)
                .eventVersion(eventVersion)
                .aggregateId(aggregateId)
                .payload(objectMapper.writeValueAsString(data))
                .build());
    }
}
