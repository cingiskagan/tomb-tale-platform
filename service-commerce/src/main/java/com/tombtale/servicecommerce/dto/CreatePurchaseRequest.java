package com.tombtale.servicecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Inbound DTO for creating a new purchase.
 *
 * <p>The {@code playerId} is accepted in the request body while JWT
 * authentication is disabled. Once the frontend integrates Zitadel,
 * this field should be extracted from the JWT {@code sub} claim instead.
 *
 * @param playerId  the purchasing player's identifier
 * @param itemCode  catalogue item code (e.g. {@code SWORD_IRON})
 * @param quantity  number of items to purchase (≥ 1)
 * @param unitPrice price per single item (≥ 0)
 */
public record CreatePurchaseRequest(
        @Schema(example = "player-001", description = "Identifier of the player making the purchase")
        @NotBlank(message = "playerId is required")
        String playerId,

        @Schema(example = "SWORD_IRON", description = "Identifier code of the virtual item")
        @NotBlank(message = "itemCode is required")
        String itemCode,

        @Schema(example = "1", description = "Number of items to purchase")
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @Schema(example = "150.00", description = "Price per single unit of the item")
        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.0", message = "unitPrice must be non-negative")
        BigDecimal unitPrice
) {
}
