package com.tombtale.serviceinventory.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.serviceinventory.config.RabbitMQConfig;
import com.tombtale.serviceinventory.dto.event.EventEnvelope;
import com.tombtale.serviceinventory.dto.event.PlayerCreatedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Creates an inventory for the character in every {@code player.created}, because inventory can't read
 * service-player's schema (ADR 0006).
 */
@Component
@RequiredArgsConstructor
public class PlayerEventConsumer {

    /** The listener's id, which a test uses to find its invocations. */
    public static final String LISTENER_ID = "inventory.player-created";

    private final CharacterInventoryService characterInventoryService;

    /**
     * Creates the inventory as {@link SystemActor#INVENTORY_PLAYER_EVENT_CONSUMER}, because no person is behind it.
     *
     * @param event the delivered {@code player.created}
     */
    @RabbitListener(id = LISTENER_ID, queues = RabbitMQConfig.PLAYER_CREATED_QUEUE)
    public void onPlayerCreated(EventEnvelope<PlayerCreatedPayload> event) {
        SystemActor.INVENTORY_PLAYER_EVENT_CONSUMER.run(() -> {
            characterInventoryService.createInventory(event.data());
            return null;
        });
    }
}
