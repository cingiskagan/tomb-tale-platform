package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.entity.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Inbound request payload for creating a purchase order.
 * <p>
 * The {@code idempotencyKey} is mandatory and must be unique per purchase
 * attempt. If a duplicate key is received, the service returns the
 * existing order instead of creating a new one (at-most-once guarantee).
 *
 * @param idempotencyKey client-generated unique key for deduplication
 * @param currency       the in-game currency used for this purchase
 * @param items          list of items to purchase (must not be empty)
 */
@Builder
public record CreatePurchaseRequest(
        @NotBlank(message = "Idempotency key is required")
        @Size(max = 64, message = "Idempotency key must not exceed 64 characters")
        String idempotencyKey,

        @NotNull(message = "Currency is required")
        Currency currency,

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<PurchaseItemRequest> items
) {
}
