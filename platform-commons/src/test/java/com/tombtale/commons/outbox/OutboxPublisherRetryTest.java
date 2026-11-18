package com.tombtale.commons.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.commons.entity.OutboxEvent;
import com.tombtale.commons.support.FixedAuthorTestBase;

/**
 * The failure the outbox exists for: RabbitMQ is down when the event goes out.
 * The row stays pending, and the next run sends it once the broker is back.
 */
@SpringBootTest
@Import({ OutboxService.class, OutboxPublisher.class })
class OutboxPublisherRetryTest extends FixedAuthorTestBase {

    private static final int HOST_PORT = freePort();
    private static final String QUEUE = "test.outbox-retry";
    private static final String EVENT_TYPE = "test.happened";
    private static final long RECEIVE_TIMEOUT_MILLIS = 5000L;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    // A restarted container gets a new random port, and the connection factory keeps the old one.
    static {
        RABBITMQ.setPortBindings(List.of(HOST_PORT + ":5672"));
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

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TestConfiguration
    static class RetryQueue {

        @Bean
        Queue retryQueue() {
            return new Queue(QUEUE, false);
        }

        @Bean
        Binding retryBinding(Queue retryQueue, TopicExchange testEventsExchange) {
            return BindingBuilder.bind(retryQueue).to(testEventsExchange).with(EVENT_TYPE);
        }
    }

    /** Other classes commit rows into the shared container, and the publisher would send them too. */
    @BeforeEach
    @AfterEach
    void emptyOutbox() {
        outboxEventRepository.deleteAllInBatch();
    }

    private OutboxEvent reloaded(OutboxEvent row) {
        return outboxEventRepository.findById(row.getId()).orElseThrow();
    }

    @Test
    void shouldSendTheRowOnceTheBrokerIsBack() {
        RABBITMQ.stop();
        OutboxEvent row = transactionTemplate.execute(status ->
                outboxService.append(EVENT_TYPE, 1, UUID.randomUUID(), Map.of("level", 1)));

        assertThat(outboxPublisher.publishPending()).isZero();
        assertThat(reloaded(row).getPublishedAt()).isNull();

        RABBITMQ.start();

        assertThat(outboxPublisher.publishPending()).isEqualTo(1);
        assertThat(reloaded(row).getPublishedAt()).isNotNull();

        Message message = rabbitTemplate.receive(QUEUE, RECEIVE_TIMEOUT_MILLIS);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(row.getPublicId().toString());
    }
}
