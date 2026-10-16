package com.tombtale.serviceaudit.service;

import com.tombtale.serviceaudit.config.RabbitMQConfig;
import com.tombtale.serviceaudit.document.StoredEvent;
import com.tombtale.serviceaudit.domain.EventAllowList;
import com.tombtale.serviceaudit.dto.event.EventEnvelope;
import com.tombtale.serviceaudit.repository.StoredEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Keeps every domain event in {@code events}. The message is acknowledged only after the insert, so a crash
 * redelivers it, and the event id that is already taken makes the redelivery write nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    /** The listener's id. */
    public static final String LISTENER_ID = "audit.events";

    private final StoredEventRepository storedEventRepository;

    /**
     * Inserts the event with only its listed fields. The names of the others are logged, never their values.
     *
     * @param event the delivered event, of any type
     */
    @RabbitListener(id = LISTENER_ID, queues = RabbitMQConfig.EVENTS_QUEUE)
    public void onEvent(EventEnvelope event) {
        Map<String, Object> kept = EventAllowList.keep(event.eventType(), event.eventVersion(), event.data());
        if (kept.size() < event.data().size()) {
            Set<String> dropped = new TreeSet<>(event.data().keySet());
            dropped.removeAll(kept.keySet());
            log.warn("Event {} ({} v{}) has fields off its allow-list, not stored: {}",
                    event.eventId(), event.eventType(), event.eventVersion(), dropped);
        }
        try {
            storedEventRepository.insert(new StoredEvent(event.eventId(), event.eventType(), event.eventVersion(),
                    event.occurredAt(), event.producer(), kept));
        } catch (DuplicateKeyException alreadyStored) {
            log.debug("Event {} is already stored", event.eventId());
        }
    }
}
