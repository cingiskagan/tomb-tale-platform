package com.tombtale.serviceinventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.serviceinventory.config.RabbitMQConfig;
import com.tombtale.serviceinventory.domain.CoinReason;
import com.tombtale.serviceinventory.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.CoinHistory;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.CoinHistoryRepository;
import com.tombtale.serviceinventory.support.PostgresTestBase;

/**
 * {@code player.created} in through a real broker, with the real auditor.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class PlayerEventConsumerTest extends PostgresTestBase {

    private static final long DELIVERY_TIMEOUT_MILLIS = 10_000L;

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    private final UUID player = UUID.randomUUID();
    private final UUID character = UUID.randomUUID();

    @Value("${inventory.starting-coins}")
    private long startingCoins;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerTestHarness harness;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private CharacterInventoryRepository characterInventoryRepository;

    @Autowired
    private CoinHistoryRepository coinHistoryRepository;

    @TestConfiguration
    @RabbitListenerTest(spy = true, capture = true)
    static class ListenerHarness {
    }

    /** The spy keeps counting across tests, so it starts each test with no invocations. */
    @AfterEach
    void removeCommittedRows() {
        PlayerEventConsumer consumerSpy = harness.getSpy(PlayerEventConsumer.LISTENER_ID);
        Mockito.reset(consumerSpy);
        amqpAdmin.purgeQueue(RabbitMQConfig.PLAYER_CREATED_DLQ, false);
        coinHistoryRepository.deleteAllInBatch();
        characterInventoryRepository.deleteAllInBatch();
    }

    @Test
    void aNewCharacterStartsWithItsCoinsAndTheirHistory() throws InterruptedException {
        send(playerCreated(character));

        assertThat(nextInvocation().getThrowable()).isNull();
        UUID consumer = SystemActor.INVENTORY_PLAYER_EVENT_CONSUMER.actorId();
        List<CharacterInventory> inventories = inventories();
        assertThat(inventories).hasSize(1);
        CharacterInventory inventory = inventories.getFirst();
        assertThat(inventory.getPlayerId()).isEqualTo(player);
        assertThat(inventory.getCoins()).isEqualTo(startingCoins);
        assertThat(inventory.getCreatedBy()).isEqualTo(consumer);

        List<CoinHistory> history = history();
        assertThat(history).hasSize(1);
        CoinHistory startingCoinsRow = history.getFirst();
        assertThat(startingCoinsRow.getCharacterInventory().getId()).isEqualTo(inventory.getId());
        assertThat(startingCoinsRow.getAmount()).isEqualTo(startingCoins);
        assertThat(startingCoinsRow.getReason()).isEqualTo(CoinReason.STARTING_COINS);
        assertThat(startingCoinsRow.getCreatedBy()).isEqualTo(consumer);
    }

    @Test
    void aRedeliveredEventAddsNothing() throws InterruptedException {
        send(playerCreated(character));
        send(playerCreated(character));

        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(inventories()).singleElement()
                .extracting(CharacterInventory::getCoins).isEqualTo(startingCoins);
        assertThat(history()).hasSize(1);
    }

    @Test
    void anEventWithNoCharacterGoesToTheDlqAtOnce() {
        PlayerEventConsumer consumerSpy = harness.getSpy(PlayerEventConsumer.LISTENER_ID);

        send(playerCreated(null));

        assertThat(rabbitTemplate.receive(RabbitMQConfig.PLAYER_CREATED_DLQ, DELIVERY_TIMEOUT_MILLIS)).isNotNull();
        verify(consumerSpy, never()).onPlayerCreated(any());
    }

    private void send(Message message) {
        rabbitTemplate.send(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, PlayerCreatedPayload.EVENT_TYPE, message);
    }

    /** A {@code player.created} as service-player sends it, for this test's player. */
    private Message playerCreated(UUID characterPublicId) {
        String characterJson = characterPublicId == null ? "null" : "\"" + characterPublicId + "\"";
        String json = """
                {
                    "eventId": "%s",
                    "eventType": "player.created",
                    "eventVersion": 1,
                    "occurredAt": "2026-11-04T10:15:30Z",
                    "producer": "service-player",
                    "data": {
                        "playerPublicId": "%s",
                        "characterPublicId": %s
                    }
                }
                """.formatted(UUID.randomUUID(), player, characterJson);
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

    private InvocationData nextInvocation() throws InterruptedException {
        InvocationData data = harness.getNextInvocationDataFor(
                PlayerEventConsumer.LISTENER_ID, DELIVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertThat(data).as("the consumer received the event").isNotNull();
        return data;
    }

    private List<CharacterInventory> inventories() {
        return characterInventoryRepository.findAll().stream()
                .filter(inventory -> character.equals(inventory.getPublicId()))
                .toList();
    }

    /** The starting coins name the character as their reference. */
    private List<CoinHistory> history() {
        return coinHistoryRepository.findAll().stream()
                .filter(row -> character.equals(row.getReference()))
                .toList();
    }
}
