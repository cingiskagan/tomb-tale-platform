package com.tombtale.servicecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.domain.PurchaseStatus;
import com.tombtale.servicecommerce.dto.event.ItemGrantRejectedPayload;
import com.tombtale.servicecommerce.dto.event.ItemGrantedPayload;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
import com.tombtale.servicecommerce.support.PostgresTestBase;

/**
 * Inventory's {@code item.granted} and {@code item.grant-rejected} through a real broker, with the real auditor, so a write outside
 * {@code SystemActor.run} fails here.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("test")
class ItemEventConsumerTest extends PostgresTestBase {

    private static final long DELIVERY_TIMEOUT_MILLIS = 10_000L;
    private static final UUID ADMIN = UUID.fromString("aaaaaaaa-0000-4000-8000-0000000000ad");

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        RABBITMQ.start();
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private RabbitListenerTestHarness harness;

    @TestConfiguration
    @RabbitListenerTest(spy = false, capture = true)
    static class ListenerHarness {
    }

    @AfterEach
    void removePurchases() {
        purchaseRepository.deleteAllInBatch();
    }

    @Test
    void aGrantCompletesThePendingPurchaseAsTheConsumer() throws InterruptedException {
        Purchase purchase = aPendingPurchase();

        rabbitTemplate.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, ItemGrantedPayload.EVENT_TYPE, itemGranted(purchase));

        assertThat(nextInvocation().getThrowable()).isNull();
        Purchase completed = purchaseRepository.findByPublicId(purchase.getPublicId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(PurchaseStatus.COMPLETED);
        assertThat(completed.getCreatedBy()).isEqualTo(ADMIN);
        assertThat(completed.getUpdatedBy()).isEqualTo(SystemActor.COMMERCE_ITEM_EVENT_CONSUMER.actorId());
    }

    @Test
    void theSameGrantTwiceCompletesThePurchaseOnce() throws InterruptedException {
        Purchase purchase = aPendingPurchase();

        rabbitTemplate.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, ItemGrantedPayload.EVENT_TYPE, itemGranted(purchase));
        rabbitTemplate.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, ItemGrantedPayload.EVENT_TYPE, itemGranted(purchase));

        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(nextInvocation().getThrowable()).isNull();
        assertThat(purchaseRepository.findByPublicId(purchase.getPublicId()).orElseThrow().getStatus())
                .isEqualTo(PurchaseStatus.COMPLETED);
    }

    @Test
    void aRejectionCancelsThePendingPurchaseOnceAsTheConsumer() throws InterruptedException {
        Purchase purchase = aPendingPurchase();

        rabbitTemplate.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, ItemGrantRejectedPayload.EVENT_TYPE, itemGrantRejected(purchase));
        rabbitTemplate.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, ItemGrantRejectedPayload.EVENT_TYPE, itemGrantRejected(purchase));

        assertThat(nextRejectedInvocation().getThrowable()).isNull();
        assertThat(nextRejectedInvocation().getThrowable()).isNull();
        Purchase cancelled = purchaseRepository.findByPublicId(purchase.getPublicId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
        assertThat(cancelled.getCreatedBy()).isEqualTo(ADMIN);
        assertThat(cancelled.getUpdatedBy()).isEqualTo(SystemActor.COMMERCE_ITEM_EVENT_CONSUMER.actorId());
    }

    /** Saved as an admin, as the POST saves it. The consumer must not need the admin's token. */
    private Purchase aPendingPurchase() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "none").claim(PublicIdClaim.CLAIM, ADMIN.toString()).build(), List.of()));
        try {
            return purchaseRepository.save(Purchase.builder()
                    .playerId(UUID.randomUUID())
                    .itemCode("SWORD_IRON")
                    .quantity(1)
                    .unitPrice(BigDecimal.TEN)
                    .totalPrice(BigDecimal.TEN)
                    .status(PurchaseStatus.PENDING)
                    .build());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static Message itemGranted(Purchase purchase) {
        String json = """
                {
                    "eventId": "%s",
                    "eventType": "item.granted",
                    "eventVersion": 1,
                    "occurredAt": "2026-10-13T10:15:30Z",
                    "producer": "service-inventory",
                    "data": { "purchasePublicId": "%s" }
                }
                """.formatted(UUID.randomUUID(), purchase.getPublicId());
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

    private static Message itemGrantRejected(Purchase purchase) {
        String json = """
                {
                    "eventId": "%s",
                    "eventType": "item.grant-rejected",
                    "eventVersion": 1,
                    "occurredAt": "2026-10-13T10:15:30Z",
                    "producer": "service-inventory",
                    "data": { "purchasePublicId": "%s", "reason": "UNKNOWN_ITEM_CODE" }
                }
                """.formatted(UUID.randomUUID(), purchase.getPublicId());
        return MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

    private InvocationData nextInvocation() throws InterruptedException {
        InvocationData invocation = harness.getNextInvocationDataFor(
                ItemEventConsumer.ITEM_GRANTED_LISTENER_ID, DELIVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertThat(invocation).as("the listener ran").isNotNull();
        return invocation;
    }

    private InvocationData nextRejectedInvocation() throws InterruptedException {
        InvocationData invocation = harness.getNextInvocationDataFor(
                ItemEventConsumer.ITEM_GRANT_REJECTED_LISTENER_ID, DELIVERY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertThat(invocation).as("the listener ran").isNotNull();
        return invocation;
    }
}
