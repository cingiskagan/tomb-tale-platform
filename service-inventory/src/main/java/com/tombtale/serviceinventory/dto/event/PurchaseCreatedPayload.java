package com.tombtale.serviceinventory.dto.event;

import java.util.Objects;
import java.util.UUID;

/**
 * The data of {@code purchase.created}, which commerce sends for every purchase an admin creates.
 *
 * @param purchasePublicId the purchase, which inventory names in its item grant
 * @param playerPublicId   the player who receives the items
 * @param itemCode         the catalog code to look up
 * @param quantity         how many copies to grant, at least 1
 */
public record PurchaseCreatedPayload(UUID purchasePublicId, UUID playerPublicId, String itemCode, Integer quantity) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "purchase.created";

    private static final int MIN_QUANTITY = 1;

    /** An event that misses a field, or grants nothing, fails conversion, so it goes to the DLQ with no retry. */
    public PurchaseCreatedPayload {
        Objects.requireNonNull(purchasePublicId, "purchasePublicId");
        Objects.requireNonNull(playerPublicId, "playerPublicId");
        Objects.requireNonNull(itemCode, "itemCode");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity < MIN_QUANTITY) {
            throw new IllegalArgumentException("quantity must be at least 1, but was " + quantity);
        }
    }
}
