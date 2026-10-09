package com.tombtale.servicecommerce.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.servicecommerce.config.RabbitMQConfig;
import com.tombtale.servicecommerce.domain.PurchaseStatus;
import com.tombtale.servicecommerce.dto.event.EventEnvelope;
import com.tombtale.servicecommerce.dto.event.PlayerCreatedPayload;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Gives every new player the beginner set, as a free purchase that is later turned into gold and items.
 * Delivery is at least once, so a repeated event finds the purchase by its key and saves nothing.
 */
@Component
@RequiredArgsConstructor
public class PlayerEventConsumer {

    /** The listener's id, which a test uses to find its invocations. */
    public static final String LISTENER_ID = "commerce.player-created";

    private static final String BEGINNER_SET_ITEM_CODE = "BEGINNER_SET";
    private static final String BEGINNER_SET_KEY = "beginner_set";

    private final PurchaseRepository purchaseRepository;

    /**
     * Saves one completed, zero-price {@code BEGINNER_SET} purchase for the player in the event.
     * No person is behind the write, so it runs as {@link SystemActor#COMMERCE_PLAYER_EVENT_CONSUMER}.
     *
     * @param event the delivered {@code player.created}
     */
    @RabbitListener(id = LISTENER_ID, queues = RabbitMQConfig.PLAYER_CREATED_QUEUE)
    public void onPlayerCreated(EventEnvelope<PlayerCreatedPayload> event) {
        if (purchaseRepository
                .existsByPlayerIdAndIdempotencyKey(event.data().playerPublicId(), BEGINNER_SET_KEY)) {
            return;
        }

        SystemActor.COMMERCE_PLAYER_EVENT_CONSUMER.run(() -> purchaseRepository.save(Purchase.builder()
                .playerId(event.data().playerPublicId())
                .itemCode(BEGINNER_SET_ITEM_CODE)
                .quantity(1)
                .unitPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .status(PurchaseStatus.COMPLETED)
                .idempotencyKey(BEGINNER_SET_KEY)
                .build()));
    }
}
