package com.tombtale.serviceaudit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
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

import com.tombtale.serviceaudit.config.RabbitMQConfig;
import com.tombtale.serviceaudit.support.MongoTestBase;

/**
 * Events in through a real broker, out in a real MongoDB that the service can only insert into.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class EventConsumerTest extends MongoTestBase {

    private static final long DELIVERY_TIMEOUT_MILLIS = 10_000L;
    private static final String PLAYER_CREATED = "player.created";
    private static final String PLAYER_DATA = """
            {"playerPublicId": "3c9a6f10-0b44-4c8e-8f37-1d2a9c7b5e60",
             "characterPublicId": "77d2b381-5e19-4a6d-9c03-2f8ab41d7e55"}""";
    private static final String PURCHASE_ID = "5b2e7a1c-9d34-4f6b-8a21-0c3e5d7f9b14";

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    private final UUID eventId = UUID.randomUUID();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @TestConfiguration
    @RabbitListenerTest(spy = true, capture = true)
    static class ListenerHarness {
    }

    @AfterEach
    void removeTheEventAndTheDeadLetters() {
        EventConsumer consumerSpy = harness.getSpy(EventConsumer.LISTENER_ID);
        Mockito.reset(consumerSpy);
        amqpAdmin.purgeQueue(RabbitMQConfig.EVENTS_DLQ, false);
        deleteStoredEvent(eventId);
    }

    @Test
    void theSameEventTwiceLeavesOneDocument() throws InterruptedException {
        Message playerCreated = event(PLAYER_CREATED, 1, PLAYER_DATA);

        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, PLAYER_CREATED, playerCreated);
        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, PLAYER_CREATED, playerCreated);

        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(nextInvocation().getThrowable()).as("the redelivery is acknowledged too").isNull();
        assertThat(storedEventCount()).isOne();
    }

    @Test
    void aFieldOffTheAllowListIsNotStored() throws InterruptedException {
        String withName = PLAYER_DATA.replace("{", "{\"displayName\": \"Player_a1b2c3d4\", ");

        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, PLAYER_CREATED, event(PLAYER_CREATED, 1, withName));

        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(storedEvent(eventId)).extractingByKey("data", InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("playerPublicId", "characterPublicId");
    }

    @ParameterizedTest
    @MethodSource("theSagaEvents")
    void everyExchangeReachesTheStore(String exchange, String eventType, String data) throws InterruptedException {
        rabbitTemplate.send(exchange, eventType, event(eventType, 1, data));

        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(storedEvent(eventId)).extractingByKey("data", InstanceOfAssertFactories.MAP)
                .containsEntry("purchasePublicId", PURCHASE_ID);
    }

    static Stream<Arguments> theSagaEvents() {
        return Stream.of(
                Arguments.of(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, "purchase.created", """
                        {"purchasePublicId": "%s", "playerPublicId": "3c9a6f10-0b44-4c8e-8f37-1d2a9c7b5e60",
                         "itemCode": "POTION_HEALTH", "quantity": 3}""".formatted(PURCHASE_ID)),
                Arguments.of(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, "item.grant-rejected", """
                        {"purchasePublicId": "%s", "reason": "UNKNOWN_ITEM_CODE"}""".formatted(PURCHASE_ID)));
    }

    @Test
    void anEventWithNoAllowListGoesToTheDlqAtOnce() throws InterruptedException {
        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, "player.renamed", event("player.renamed", 1, PLAYER_DATA));

        assertThat(rabbitTemplate.receive(RabbitMQConfig.EVENTS_DLQ, DELIVERY_TIMEOUT_MILLIS)).isNotNull();
        assertThat(nextInvocation().getThrowable()).isNotNull();
        assertThat(harness.getNextInvocationDataFor(EventConsumer.LISTENER_ID, 1L, TimeUnit.SECONDS))
                .as("no retry").isNull();
        assertThat(storedEvent(eventId)).isNull();
    }

    @Test
    void anEventWithNoEventIdGoesToTheDlqAtOnce() {
        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, PLAYER_CREATED, json("""
                {"eventType": "player.created", "eventVersion": 1, "occurredAt": "2026-10-16T10:15:30Z",
                 "producer": "service-player", "data": {}}"""));

        assertThat(rabbitTemplate.receive(RabbitMQConfig.EVENTS_DLQ, DELIVERY_TIMEOUT_MILLIS)).isNotNull();
        EventConsumer consumerSpy = harness.getSpy(EventConsumer.LISTENER_ID);
        verify(consumerSpy, never()).onEvent(any());
    }

    private Message event(String eventType, int eventVersion, String data) {
        return json("""
                {"eventId": "%s", "eventType": "%s", "eventVersion": %d, "occurredAt": "2026-10-16T10:15:30Z",
                 "producer": "a-producer", "data": %s}""".formatted(eventId, eventType, eventVersion, data));
    }

    private static Message json(String body) {
        return MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).build();
    }

    private InvocationData nextInvocation() throws InterruptedException {
        InvocationData invocation = harness.getNextInvocationDataFor(
                EventConsumer.LISTENER_ID, DELIVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertThat(invocation).as("the listener ran").isNotNull();
        return invocation;
    }
}
