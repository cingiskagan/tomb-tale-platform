package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.entity.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only DTO returned to API consumers for purchase data.
 *
 * <p>Intentionally omits the internal JPA {@code version} field
 * to keep the public contract clean.
 *
 * @param id          unique purchase identifier
 * @param playerId    Zitadel subject of the purchasing player
 * @param itemCode    catalogue item code
 * @param quantity    number of items purchased
 * @param unitPrice   price per single item
 * @param totalPrice  {@code quantity × unitPrice}
 * @param status      current lifecycle state
 * @param purchasedAt UTC timestamp of the original purchase
 */
public record PurchaseResponse(
        UUID id,
        String playerId,
        String itemCode,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        PurchaseStatus status,
        Instant purchasedAt
) {
}
