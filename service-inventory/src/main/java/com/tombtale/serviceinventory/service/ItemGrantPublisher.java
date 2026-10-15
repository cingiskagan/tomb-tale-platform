package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.config.RabbitMQConfig;
import com.tombtale.serviceinventory.dto.event.EventEnvelope;
import com.tombtale.serviceinventory.dto.event.ItemGrantPayload;
import com.tombtale.serviceinventory.entity.ItemGrant;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Sends an item grant to {@code item.events} and waits until the broker confirms it.
 */
@Component
public class ItemGrantPublisher {

    private static final String PRODUCER = "service-inventory";
    private static final int EVENT_VERSION = 1;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Duration confirmTimeout;

    /** The confirm timeout comes from {@code app.publish} in application.yml. */
    public ItemGrantPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${app.publish.confirm-timeout}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.confirmTimeout = confirmTimeout;
    }

    /**
     * Sends the item grant as its outcome's event type. Its {@code eventId} is the item grant's {@code publicId},
     * so an item grant sent twice carries the same id.
     *
     * @param itemGrant the saved item grant
     * @throws org.springframework.amqp.AmqpException if the broker does not confirm the message
     */
    public void publish(ItemGrant itemGrant) {
        String eventType = itemGrant.getOutcome().eventType();
        EventEnvelope<ItemGrantPayload> envelope = new EventEnvelope<>(
                itemGrant.getPublicId(),
                eventType,
                EVENT_VERSION,
                itemGrant.getCreatedAt(),
                PRODUCER,
                new ItemGrantPayload(itemGrant.getPurchaseId(), itemGrant.getReason()));
        Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(envelope))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(itemGrant.getPublicId().toString())
                .build();

        rabbitTemplate.invoke(operations -> {
            operations.send(RabbitMQConfig.ITEM_EVENTS_EXCHANGE, eventType, message);
            operations.waitForConfirmsOrDie(confirmTimeout.toMillis());
            return null;
        });
    }
}
