package com.tombtale.serviceinventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.serviceinventory.config.RabbitMQConfig;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import com.tombtale.serviceinventory.support.PostgresTestBase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code purchase.created} in and the item grant out, through a real broker, with the real auditor.
 * A test queue on {@code item.events} stands in for commerce.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class PurchaseEventConsumerTest extends PostgresTestBase {

    private static final long RECEIVE_TIMEOUT_MILLIS = 10_000L;
    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-4000-8000-0000000000ad");
    private static final String POTION = "POTION_HEALTH";
    private static final String POISON = "POTION_POISON";
    private static final int POTION_STACK = 10;
    private static final int QUANTITY = 3;

    private static final String EVENT_ID = "eventId";
    private static final String DATA = "data";

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    private final UUID player = UUID.randomUUID();
    /** Not auto-delete, because a timed receive cancels its consumer, and that would delete the queue. */
    private final Queue itemGrants = QueueBuilder.nonDurable("inventory-test.item-grants." + UUID.randomUUID()).build();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TopicExchange itemEventsExchange;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemTemplateRepository itemTemplateRepository;

    @Autowired
    private ItemInstanceRepository itemInstanceRepository;

    @Autowired
    private ItemGrantRepository itemGrantRepository;

    @BeforeEach
    void aCatalogAndAQueueForTheItemGrants() {
        addItemTemplate(POTION);

        amqpAdmin.declareQueue(itemGrants);
        amqpAdmin.declareBinding(BindingBuilder.bind(itemGrants).to(itemEventsExchange).with("item.#"));
    }

    /** Item sets write templates as an admin, so the test does too. */
    private void addItemTemplate(String itemCode) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "none").claim(PublicIdClaim.CLAIM, ADMIN.toString()).build(), List.of()));
        itemTemplateRepository.save(ItemTemplate.builder().code(itemCode).maxStackSize(POTION_STACK).build());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void removeCommittedRows() {
        amqpAdmin.deleteQueue(itemGrants.getName());
        itemInstanceRepository.deleteAllInBatch();
        itemGrantRepository.deleteAllInBatch();
        itemTemplateRepository.deleteAllInBatch();
    }

    @Test
    void aPurchaseGetsItsItemGrantAsTheConsumer() {
        UUID purchase = UUID.randomUUID();

        rabbitTemplate.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, PurchaseCreatedPayload.EVENT_TYPE,
                purchaseCreated(purchase, QUANTITY, POTION));

        JsonNode sent = nextItemGrant();
        ItemGrant itemGrant = itemGrantRepository.findByPurchaseId(purchase).orElseThrow();
        assertThat(sent.get("eventType").asString()).isEqualTo("item.granted");
        assertThat(sent.get(EVENT_ID).asString()).isEqualTo(itemGrant.getPublicId().toString());
        assertThat(sent.get(DATA).get("purchasePublicId").asString()).isEqualTo(purchase.toString());
        assertThat(sent.get(DATA).has("reason")).as("a granted item grant has no reason").isFalse();
        assertThat(itemGrant.getCreatedBy()).isEqualTo(SystemActor.INVENTORY_PURCHASE_EVENT_CONSUMER.actorId());
        assertThat(stackSizes()).containsExactly(QUANTITY);
    }

    @Test
    void aRedeliveredPurchaseGetsTheSameItemGrantAgain() {
        UUID purchase = UUID.randomUUID();

        rabbitTemplate.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, PurchaseCreatedPayload.EVENT_TYPE,
                purchaseCreated(purchase, QUANTITY, POTION));
        rabbitTemplate.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, PurchaseCreatedPayload.EVENT_TYPE,
                purchaseCreated(purchase, QUANTITY, POTION));

        JsonNode first = nextItemGrant();
        JsonNode second = nextItemGrant();
        assertThat(second.get(EVENT_ID)).isEqualTo(first.get(EVENT_ID));
        assertThat(stackSizes()).containsExactly(QUANTITY);
    }

    @Test
    void aRedeliveredPurchaseStaysRejectedAfterItsCodeJoinsTheCatalog() {
        UUID purchase = UUID.randomUUID();

        rabbitTemplate.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, PurchaseCreatedPayload.EVENT_TYPE,
                purchaseCreated(purchase, QUANTITY, POISON));

        JsonNode sent = nextItemGrant();
        ItemGrant itemGrant = itemGrantRepository.findByPurchaseId(purchase).orElseThrow();
        assertThat(sent.get("eventType").asString()).isEqualTo("item.grant-rejected");
        assertThat(sent.get(EVENT_ID).asString()).isEqualTo(itemGrant.getPublicId().toString());
        assertThat(sent.get(DATA).get("purchasePublicId").asString()).isEqualTo(purchase.toString());
        assertThat(sent.get(DATA).get("reason").asString()).isEqualTo("UNKNOWN_ITEM_CODE");
        assertThat(itemGrant.getCreatedBy()).isEqualTo(SystemActor.INVENTORY_PURCHASE_EVENT_CONSUMER.actorId());
        assertThat(stackSizes()).isEmpty();

        addItemTemplate(POISON);

        rabbitTemplate.send(RabbitMQConfig.PURCHASE_EVENTS_EXCHANGE, PurchaseCreatedPayload.EVENT_TYPE,
                purchaseCreated(purchase, QUANTITY, POISON));

        JsonNode sentAfterTemplateAddition = nextItemGrant();

        assertThat(sentAfterTemplateAddition.get("eventType").asString()).isEqualTo("item.grant-rejected");
        assertThat(sentAfterTemplateAddition.get(EVENT_ID).asString()).isEqualTo(itemGrant.getPublicId().toString());
        assertThat(stackSizes()).isEmpty();
    }

    private Message purchaseCreated(UUID purchase, int quantity, String itemCode) {
        String json = """
                {
                    "eventId": "%s",
                    "eventType": "purchase.created",
                    "eventVersion": 1,
                    "occurredAt": "2026-10-13T10:15:30Z",
                    "producer": "service-commerce",
                    "data": {
                        "purchasePublicId": "%s",
                        "playerPublicId": "%s",
                        "itemCode": "%s",
                        "quantity": %d
                    }
                }
                """.formatted(UUID.randomUUID(), purchase, player, itemCode, quantity);
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

    private JsonNode nextItemGrant() {
        Message message = rabbitTemplate.receive(itemGrants.getName(), RECEIVE_TIMEOUT_MILLIS);
        assertThat(message).as("inventory sent an item grant").isNotNull();
        return objectMapper.readTree(message.getBody());
    }

    private List<Integer> stackSizes() {
        return itemInstanceRepository.findAll().stream()
                .filter(stack -> player.equals(stack.getPlayerId()))
                .map(ItemInstance::getQuantity)
                .toList();
    }
}
