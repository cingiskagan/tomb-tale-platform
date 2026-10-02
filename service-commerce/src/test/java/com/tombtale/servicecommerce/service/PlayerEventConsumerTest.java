package com.tombtale.servicecommerce.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.RabbitListenerTest;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness;
import org.springframework.amqp.rabbit.test.RabbitListenerTestHarness.InvocationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
import com.tombtale.servicecommerce.support.PostgresTestBase;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class PlayerEventConsumerTest extends PostgresTestBase {

    private static final int DELIVERY_TIMEOUT_SECONDS = 10;

    private static final String PLAYER_ID = "3c9a6f10-0b44-4c8e-8f37-1d2a9c7b5e60";

    String jsonString = """
            {
                "eventId": "8b1f0c4e-2a77-4f2e-9a2c-6d0f1b3c5e21",
                "eventType": "player.created",
                "eventVersion": 1,
                "occurredAt": "2026-09-23T10:15:30Z",
                "producer": "service-player",
                "data": {
                    "playerPublicId": "%s",
                    "displayName": "Player_a1b2c3d4",
                    "characterPublicId": "77d2b381-5e19-4a6d-9c03-2f8ab41d7e55"
                }
            }
            """.formatted(PLAYER_ID);

    Message message = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8))
            .setContentType(MessageProperties.CONTENT_TYPE_JSON).build();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private RabbitListenerTestHarness harness;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    @TestConfiguration
    @RabbitListenerTest(capture = true)
    static class ListenerHarness {
    }

    @AfterEach
    void removePurchases() {
        purchaseRepository.deleteAllInBatch();
    }

    @Test
    void sameEventTwiceGivesOnePurchase() throws InterruptedException {

        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, "player.created", message);
        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, "player.created", message);

        InvocationData data1 = harness
                .getNextInvocationDataFor(PlayerEventConsumer.LISTENER_ID, DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        InvocationData data2 = harness
                .getNextInvocationDataFor(PlayerEventConsumer.LISTENER_ID, DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(data1).isNotNull();
        assertThat(data2).isNotNull();
        assertThat(data1.getThrowable()).isNull();
        assertThat(data2.getThrowable()).isNull();

        List<Purchase> receivedPurchases = purchaseRepository.findAll().stream()
                .filter(purchase -> PLAYER_ID.equals(purchase.getPlayerId().toString())).toList();

        assertThat(receivedPurchases).hasSize(1);
    }
}
