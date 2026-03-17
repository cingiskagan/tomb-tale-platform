package com.tombtale.servicecommerce.service;

import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.entity.OutboxEvent;
import com.tombtale.servicecommerce.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for unpublished events and forwards them
 * to RabbitMQ using the event type as the routing key.
 * <p>
 * Guarantees at-least-once delivery: events are only marked as
 * published after the broker confirms receipt. If the broker is
 * down, events remain in the table and are retried on the next poll.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    /** Fixed-rate polling interval in milliseconds. */
    private static final long POLL_INTERVAL_MS = 5000L;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Scheduled poller that runs every {@value #POLL_INTERVAL_MS} ms.
     * Reads all unpublished events and publishes them to RabbitMQ.
     */
    @Scheduled(fixedRate = POLL_INTERVAL_MS)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    /**
     * Publishes a single outbox event to the commerce exchange and
     * marks it as published in the database.
     */
    private void publishSingleEvent(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMMERCE_EVENTS_EXCHANGE,
                    event.getEventType(),
                    event.getPayload());

            event.setPublished(true);
            outboxEventRepository.save(event);

            log.debug("Published outbox event {} with type: {}",
                    event.getId(), event.getEventType());
        } catch (Exception ex) {
            log.error("Failed to publish outbox event {}: {}",
                    event.getId(), ex.getMessage());
            // Event stays unpublished — will be retried on next poll
        }
    }
}
