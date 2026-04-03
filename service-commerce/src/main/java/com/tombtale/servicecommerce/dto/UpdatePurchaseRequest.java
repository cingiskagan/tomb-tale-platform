package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.entity.PurchaseStatus;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Inbound DTO for partially updating an existing purchase.
 *
 * <p>All fields are nullable — only non-null fields are applied.
 * When {@code quantity} is updated, the service automatically
 * recalculates {@code totalPrice}.
 *
 * @param status   new lifecycle state (nullable — skipped if null)
 * @param quantity new item count, must be ≥ 1 if provided (nullable)
 */
public record UpdatePurchaseRequest(
        @Schema(example = "REFUNDED", description = "Optional update to the transaction lifecycle state")
        PurchaseStatus status,

        @Schema(example = "2", description = "Optional update to the quantity purchased")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {
}
