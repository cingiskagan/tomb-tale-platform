package com.tombtale.servicecommerce.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.dto.event.EventEnvelope;
import com.tombtale.servicecommerce.entity.OutboxEvent;
import com.tombtale.servicecommerce.repository.OutboxEventRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.ReceiverContext;
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
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
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

    private static final String PRODUCER = "service-commerce";

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObservationRegistry observationRegistry;
    private final int batchSize;
    private final Duration confirmTimeout;
    private final Duration retention;

    /** The batch size, confirm timeout and retention come from {@code app.outbox} in application.yml. */
    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            ObservationRegistry observationRegistry,
            @Value("${app.outbox.batch-size}") int batchSize,
            @Value("${app.outbox.confirm-timeout}") Duration confirmTimeout,
            @Value("${app.outbox.retention}") Duration retention) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.observationRegistry = observationRegistry;
        this.batchSize = batchSize;
        this.confirmTimeout = confirmTimeout;
        this.retention = retention;
    }

    /**
     * Publishes the oldest pending rows in id order and stops at the first failure.
     * The rows sent before it still commit as published, by {@link SystemActor#COMMERCE_OUTBOX_PUBLISHER}.
     *
     * @return how many rows were published
     */
    @Scheduled(fixedDelayString = "${app.outbox.publish-interval}")
    public int publishPending() {
        return SystemActor.COMMERCE_OUTBOX_PUBLISHER.run(() -> transactionTemplate.execute(status -> publishBatch()));
    }

    private int publishBatch() {
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
        EventEnvelope<JsonNode> envelope = new EventEnvelope<>(
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

        // Micrometer reads a trace only from a carrier, usually a message. Here the row is the carrier,
        // so this span continues the trace that wrote the row, and the send inside it passes the trace on.
        ReceiverContext<OutboxEvent> trace = new ReceiverContext<>(
                (row, key) -> OutboxEvent.TRACEPARENT.equals(key) ? row.getTraceparent() : null);
        trace.setCarrier(event);
        Observation.createNotStarted("outbox.publish", () -> trace, observationRegistry)
                .lowCardinalityKeyValue("event.type", event.getEventType())
                .observe(() -> rabbitTemplate.invoke(operations -> {
                    operations.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, event.getEventType(), message);
                    operations.waitForConfirmsOrDie(confirmTimeout.toMillis());
                    return null;
                }));
    }
}
