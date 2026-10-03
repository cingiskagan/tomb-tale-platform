package com.tombtale.serviceplayer.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

/**
 * The broker definitions in {@code infrastructure/rabbitmq} keep a player event that no queue binds.
 * The test loads that same file, so a change to it that loses such events fails here.
 */
class PlayerEventsAlternateExchangeTest {

    private static final String DEFINITIONS = "/tmp/definitions.json";
    private static final String UNROUTED_QUEUE = "player.events.unrouted";
    private static final String UNBOUND_ROUTING_KEY = "player.renamed";
    private static final long RECEIVE_TIMEOUT_MILLIS = 5_000L;

    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../infrastructure/rabbitmq/definitions.json"), DEFINITIONS);

    private static CachingConnectionFactory connectionFactory;

    static {
        RABBITMQ.start();
    }

    /** Loads the file as compose does, then declares the exchange as service-player does. */
    @BeforeAll
    static void loadDefinitions() throws Exception {
        ExecResult result = RABBITMQ.execInContainer("rabbitmqctl", "import_definitions", DEFINITIONS);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();

        connectionFactory = new CachingConnectionFactory(RABBITMQ.getHost(), RABBITMQ.getAmqpPort());
        connectionFactory.setUsername(RABBITMQ.getAdminUsername());
        connectionFactory.setPassword(RABBITMQ.getAdminPassword());
        new RabbitAdmin(connectionFactory).declareExchange(new RabbitMQConfig().playerEventsExchange());
    }

    @AfterAll
    static void closeConnection() {
        connectionFactory.destroy();
    }

    @Test
    void anEventThatNoQueueBindsLandsInTheUnroutedQueue() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        rabbitTemplate.convertAndSend(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, UNBOUND_ROUTING_KEY, "event");

        Message message = rabbitTemplate.receive(UNROUTED_QUEUE, RECEIVE_TIMEOUT_MILLIS);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo(UNBOUND_ROUTING_KEY);
    }
}
