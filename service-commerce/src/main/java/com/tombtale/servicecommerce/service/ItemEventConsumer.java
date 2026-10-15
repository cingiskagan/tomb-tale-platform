package com.tombtale.servicecommerce.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.dto.event.EventEnvelope;
import com.tombtale.servicecommerce.dto.event.ItemGrantRejectedPayload;
import com.tombtale.servicecommerce.dto.event.ItemGrantedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Applies inventory's item grant to a pending purchase, which ends the purchase saga (ADR 0027).
 * No person is behind these writes, so they run as {@link SystemActor#COMMERCE_ITEM_EVENT_CONSUMER}.
 */
@Component
@RequiredArgsConstructor
public class ItemEventConsumer {

    /** The listener's id, which a test uses to find its invocations. */
    public static final String ITEM_GRANTED_LISTENER_ID = "commerce.item-granted";
    /** The rejection listener's id, which a test uses to find its invocations. */
    public static final String ITEM_GRANT_REJECTED_LISTENER_ID = "commerce.item-grant-rejected";

    private final PurchaseService purchaseService;

    /**
     * Completes the purchase that inventory granted.
     *
     * @param event the delivered {@code item.granted}
     */
    @RabbitListener(id = ITEM_GRANTED_LISTENER_ID, queues = RabbitMQConfig.ITEM_GRANTED_QUEUE)
    public void onItemGranted(EventEnvelope<ItemGrantedPayload> event) {
        SystemActor.COMMERCE_ITEM_EVENT_CONSUMER.run(() -> {
            purchaseService.completeGrantedPurchase(event.data().purchasePublicId());
            return null;
        });
    }

    /**
     * Cancels the purchase that inventory refused. This is the saga's compensation.
     *
     * @param event the delivered {@code item.grant-rejected}
     */
    @RabbitListener(id = ITEM_GRANT_REJECTED_LISTENER_ID, queues = RabbitMQConfig.ITEM_GRANT_REJECTED_QUEUE)
    public void onItemGrantRejected(EventEnvelope<ItemGrantRejectedPayload> event) {
        SystemActor.COMMERCE_ITEM_EVENT_CONSUMER.run(() -> {
            purchaseService.cancelRejectedPurchase(event.data().purchasePublicId(), event.data().reason());
            return null;
        });
    }
}
