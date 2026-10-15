package com.tombtale.serviceinventory.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.serviceinventory.config.RabbitMQConfig;
import com.tombtale.serviceinventory.dto.event.EventEnvelope;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.ItemGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Inventory's step in the purchase saga (ADR 0027). It needs no outbox: the purchase is acknowledged only after
 * the broker confirms the item grant, so a crash between the two redelivers it, and the saved item grant goes out again.
 */
@Component
@RequiredArgsConstructor
public class PurchaseEventConsumer {

    /** The listener's id. */
    public static final String LISTENER_ID = "inventory.purchase-created";

    private final ItemGrantService itemGrantService;
    private final ItemGrantPublisher itemGrantPublisher;

    /**
     * Commits the item grant as {@link SystemActor#INVENTORY_PURCHASE_EVENT_CONSUMER}, then sends it.
     *
     * @param event the delivered {@code purchase.created}
     */
    @RabbitListener(id = LISTENER_ID, queues = RabbitMQConfig.PURCHASE_CREATED_QUEUE)
    public void onPurchaseCreated(EventEnvelope<PurchaseCreatedPayload> event) {
        ItemGrant itemGrant = SystemActor.INVENTORY_PURCHASE_EVENT_CONSUMER.run(
                () -> itemGrantService.findOrCreateItemGrant(event.data()));
        itemGrantPublisher.publish(itemGrant);
    }
}
