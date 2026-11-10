package com.tombtale.servicecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound DTO for creating a new purchase.
 *
 * <p>{@code playerId} names the buyer and comes from the body, not from the
 * token: this endpoint is admin-only and an admin creates purchases on behalf
 * of a player, so the caller and the buyer are different people. See ADR 0011.
 *
 * <p>It is the player's {@code publicId}, which the caller already holds —
 * the portal lists players from service-player before it opens this form.
 * Commerce stores it without checking that such a player exists (ADR 0014).
 *
 * @param playerId  the buying player's publicId
 * @param itemCode  catalogue item code (e.g. {@code SWORD_IRON})
 * @param quantity  number of items to purchase (≥ 1)
 * @param unitPrice price per single item (≥ 0)
 */
public record CreatePurchaseRequest(
        @NotNull(message = "playerId is required")
        UUID playerId,

        @NotBlank(message = "itemCode is required")
        String itemCode,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.0", message = "unitPrice must be non-negative")
        BigDecimal unitPrice
) {
}
