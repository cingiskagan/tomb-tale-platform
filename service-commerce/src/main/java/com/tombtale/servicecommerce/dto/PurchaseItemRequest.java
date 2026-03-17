package com.tombtale.servicecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * A single line-item within a {@link CreatePurchaseRequest}.
 *
 * @param itemCatalogId reference to the game item catalog
 * @param itemName      human-readable name of the item
 * @param quantity      number of units to purchase
 * @param unitPrice     price per unit in the selected currency
 */
@Builder
public record PurchaseItemRequest(
        @NotBlank(message = "Item catalog ID is required")
        String itemCatalogId,

        @NotBlank(message = "Item name is required")
        String itemName,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be positive")
        BigDecimal unitPrice
) {
}
