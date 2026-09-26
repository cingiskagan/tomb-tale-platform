package com.tombtale.serviceplayer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.support.PostgresTestBase;

/**
 * The failure the outbox exists for: RabbitMQ is down when the event goes out.
 * The row stays pending, and the next run sends it once the broker is back.
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherRetryTest extends PostgresTestBase {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final int HOST_PORT = freePort();
    private static final String QUEUE = "test.player-created";
    private static final Long RECEIVE_TIMEOUT = 5000L;
    private static final String EVENT_TYPE = PlayerCreatedPayload.EVENT_TYPE;

    private String subject;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    // A restarted container gets a new random port, and the connection factory keeps the old one.
    static {
        RABBITMQ.setPortBindings(List.of(HOST_PORT + ":5672"));
        RABBITMQ.start();
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private OutboxEvent reloaded(OutboxEvent row) {
        return outboxEventRepository.findById(row.getId()).orElseThrow();
    }

    @BeforeEach
    void newSubject() {
        subject = "outbox-retry-" + UUID.randomUUID();
    }

    @AfterEach
    void removeCommittedOutboxEvents() {
        playerRepository.findByZitadelUserIdWithCharacters(subject).ifPresent(player -> {
            outboxEventRepository.deleteAll(outboxEventRepository.findAll().stream()
                    .filter(event -> event.getAggregateId().equals(player.getPublicId()))
                    .toList());
            playerRepository.delete(player);
        });
    }

    @TestConfiguration
    static class RabbitMQTestConfiguration {

        @Bean
        Queue playerCreatedQueue() {
            return new Queue(QUEUE, false);
        }

        @Bean
        Binding playerCreatedBinding(Queue playerCreatedQueue, TopicExchange playerEventsExchange) {
            return BindingBuilder
                    .bind(playerCreatedQueue)
                    .to(playerEventsExchange)
                    .with("player.created");
        }
    }

    @Test
    void shouldSendRowAfterBrokerComesBack() {
        outboxEventRepository.deleteAllInBatch();
        RABBITMQ.stop();

        PlayerResponse player = playerService.getOrCreatePlayer(subject);

        int sentCount = outboxPublisher.publishPending();

        OutboxEvent pendingEvent = outboxEventRepository.findAll().stream()
                .filter(event -> player.publicId().equals(event.getAggregateId())
                                    && EVENT_TYPE.equals(event.getEventType()))
                .toList().getFirst();

        assertThat(sentCount).isEqualTo(0);
        assertThat(reloaded(pendingEvent).getPublishedAt()).isNull();

        RABBITMQ.start();

        sentCount = outboxPublisher.publishPending();

        assertThat(sentCount).isEqualTo(1);
        assertThat(reloaded(pendingEvent).getPublishedAt()).isNotNull();

        Message message = rabbitTemplate.receive(QUEUE, RECEIVE_TIMEOUT);

        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(pendingEvent.getPublicId().toString());
    }

}
