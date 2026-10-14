package com.tombtale.servicecommerce.dto.event;

import java.util.UUID;

/**
 * The data of {@code purchase.created}: what inventory needs to grant the items, and nothing about the price.
 *
 * @param purchasePublicId the purchase, which inventory names in its item grant
 * @param playerPublicId   the player who receives the items
 * @param itemCode         the catalog code inventory looks up
 * @param quantity         how many copies to grant
 */
public record PurchaseCreatedPayload(UUID purchasePublicId, UUID playerPublicId, String itemCode, int quantity) {

    /** The event type, which is also the routing key. */
    public static final String EVENT_TYPE = "purchase.created";

    /** The version of this payload's shape. */
    public static final int EVENT_VERSION = 1;
}
