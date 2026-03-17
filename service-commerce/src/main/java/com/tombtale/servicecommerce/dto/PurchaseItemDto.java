package com.tombtale.servicecommerce.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only DTO for a line-item within a purchase order response.
 *
 * @param id            item unique identifier
 * @param itemCatalogId reference to the game item catalog
 * @param itemName      human-readable item name
 * @param quantity      number of units purchased
 * @param unitPrice     price per unit
 */
@Builder
public record PurchaseItemDto(
        UUID id,
        String itemCatalogId,
        String itemName,
        int quantity,
        BigDecimal unitPrice
) {
}
