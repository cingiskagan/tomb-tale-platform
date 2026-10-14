package com.tombtale.servicecommerce.dto.event;

import java.util.Objects;
import java.util.UUID;

/**
 * The data of {@code item.granted}: inventory added the purchase's items to the player's stacks.
 *
 * @param purchasePublicId the purchase that inventory granted
 */
public record ItemGrantedPayload(UUID purchasePublicId) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "item.granted";

    /** An event with no purchase fails conversion, so it goes to the DLQ with no retry. */
    public ItemGrantedPayload {
        Objects.requireNonNull(purchasePublicId, "purchasePublicId");
    }
}
