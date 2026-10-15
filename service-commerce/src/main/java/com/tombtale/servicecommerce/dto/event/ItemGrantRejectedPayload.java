package com.tombtale.servicecommerce.dto.event;

import java.util.Objects;
import java.util.UUID;

/**
 * The data of {@code item.grant-rejected}: inventory refused the purchase and added nothing.
 *
 * @param purchasePublicId the purchase that inventory refused
 * @param reason           why, such as {@code UNKNOWN_ITEM_CODE}, kept as inventory sent it
 */
public record ItemGrantRejectedPayload(UUID purchasePublicId, String reason) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "item.grant-rejected";

    /** An event with no purchase fails conversion, so it goes to the DLQ with no retry. */
    public ItemGrantRejectedPayload {
        Objects.requireNonNull(purchasePublicId, "purchasePublicId");
    }
}
