package com.tombtale.serviceinventory.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * The data of an item grant, sent as {@code item.granted} or {@code item.grant-rejected}.
 * A granted item grant has no reason, so the field is left out of its JSON.
 *
 * @param purchasePublicId the purchase this item grant is for
 * @param reason           why inventory refused, such as {@code UNKNOWN_ITEM_CODE}, or null when granted
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemGrantPayload(UUID purchasePublicId, String reason) {
}
