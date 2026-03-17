package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.entity.Currency;
import com.tombtale.servicecommerce.entity.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only DTO returned by API endpoints.
 * Prevents JPA entity internals (version, lazy proxies) from leaking
 * into the HTTP response.
 *
 * @param id             order unique identifier
 * @param idempotencyKey client-supplied deduplication key
 * @param status         current lifecycle status of the order
 * @param totalPrice     total cost of the order
 * @param currency       currency used for the purchase
 * @param items          line-items in this order
 * @param createdAt      timestamp when the order was created
 * @param updatedAt      timestamp of the last modification
 */
@Builder
public record PurchaseOrderDto(
        UUID id,
        String idempotencyKey,
        OrderStatus status,
        BigDecimal totalPrice,
        Currency currency,
        List<PurchaseItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {
}
