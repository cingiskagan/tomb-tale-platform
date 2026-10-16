package com.tombtale.serviceplayer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.support.FixedAuthorTestBase;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * The publisher sends a row later, in its own job, and the message still carries the trace that wrote the row.
 * Tests turn trace export off, and Spring Boot then propagates no trace, so this class turns it back on.
 */
@SpringBootTest
@AutoConfigureTracing
@ActiveProfiles("test")
class OutboxTraceTest extends FixedAuthorTestBase {

    private static final String QUEUE = "test.outbox-trace";
    private static final String EVENT_TYPE = PlayerCreatedPayload.EVENT_TYPE;
    private static final long RECEIVE_TIMEOUT_MILLIS = 5000L;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Tracer tracer;

    @TestConfiguration
    static class TraceQueue {

        @Bean
        Queue traceQueue() {
            return new Queue(QUEUE, false);
        }

        @Bean
        Binding traceBinding(Queue traceQueue, TopicExchange playerEventsExchange) {
            return BindingBuilder.bind(traceQueue).to(playerEventsExchange).with(EVENT_TYPE);
        }
    }

    /** Other classes commit rows into the shared container, and the publisher would send them too. */
    @BeforeEach
    void emptyOutbox() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void theMessageCarriesTheTraceThatWroteTheRow() {
        Span request = tracer.nextSpan().name("request").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(request)) {
            transactionTemplate.executeWithoutResult(status ->
                    outboxService.append(EVENT_TYPE, 1, UUID.randomUUID(), Map.of("level", 1)));
        } finally {
            request.end();
        }
        String traceId = request.context().traceId();

        assertThat(outboxEventRepository.findAll()).singleElement()
                .extracting(OutboxEvent::getTraceparent).asString().contains(traceId);

        assertThat(outboxPublisher.publishPending()).isEqualTo(1);
        Message message = rabbitTemplate.receive(QUEUE, RECEIVE_TIMEOUT_MILLIS);

        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().<String>getHeader(OutboxEvent.TRACEPARENT)).contains(traceId);
    }
}
