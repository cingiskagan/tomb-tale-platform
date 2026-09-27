package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.config.RabbitMQConfig;
import com.tombtale.serviceplayer.dto.event.EventEnvelope;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sends pending outbox rows to RabbitMQ and marks each one once the broker confirms it.
 * A failed send leaves that row, and every row behind it, pending for the next run.
 */
@Slf4j
@Component
public class OutboxPublisher {

    private static final String PRODUCER = "service-player";

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final Duration confirmTimeout;
    private final Duration retention;

    /** The batch size, confirm timeout and retention come from {@code app.outbox} in application.yml. */
    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.outbox.batch-size}") int batchSize,
            @Value("${app.outbox.confirm-timeout}") Duration confirmTimeout,
            @Value("${app.outbox.retention}") Duration retention) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.confirmTimeout = confirmTimeout;
        this.retention = retention;
    }

    /**
     * Publishes the oldest pending rows in id order and stops at the first failure.
     * The rows sent before it still commit as published.
     *
     * @return how many rows were published
     */
    @Scheduled(fixedDelayString = "${app.outbox.publish-interval}")
    @Transactional
    public int publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingForUpdate(Limit.of(batchSize));
        int published = 0;
        for (OutboxEvent event : pending) {
            try {
                send(event);
            } catch (AmqpException e) {
                log.warn("RabbitMQ did not confirm event {}, so {} event(s) stay pending: {}",
                        event.getPublicId(), pending.size() - published, e.getMessage());
                break;
            }
            event.setPublishedAt(Instant.now());
            published++;
        }
        return published;
    }

    /**
     * Deletes the rows published longer ago than the retention.
     *
     * @return how many rows were deleted
     */
    @Scheduled(cron = "${app.outbox.purge-cron}")
    @Transactional
    public int purgePublished() {
        return outboxEventRepository.deletePublishedBefore(Instant.now().minus(retention));
    }

    private void send(OutboxEvent event) {
        EventEnvelope envelope = new EventEnvelope(
                event.getPublicId(),
                event.getEventType(),
                event.getEventVersion(),
                event.getCreatedAt(),
                PRODUCER,
                objectMapper.readTree(event.getPayload()));
        Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(envelope))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(event.getPublicId().toString())
                .build();

        rabbitTemplate.invoke(operations -> {
            operations.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, event.getEventType(), message);
            operations.waitForConfirmsOrDie(confirmTimeout.toMillis());
            return null;
        });
    }
}
