package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.domain.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only DTO returned to API consumers for purchase data.
 *
 * <p>Intentionally omits the internal JPA {@code version} field
 * to keep the public contract clean.
 *
 * <p>Every field is named after the entity field behind it (ADR 0017). The
 * internal key never leaves the persistence layer, so {@code publicId} is the
 * only identifier here.
 *
 * @param publicId   the purchase's public identifier
 * @param playerId   publicId of the purchasing player
 * @param itemCode   catalogue item code
 * @param quantity   number of items purchased
 * @param unitPrice  price per single item
 * @param totalPrice {@code quantity × unitPrice}
 * @param status     current lifecycle state
 * @param createdAt  UTC timestamp of the purchase
 */
public record PurchaseResponse(
        UUID publicId,
        UUID playerId,
        String itemCode,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        PurchaseStatus status,
        Instant createdAt
) {
}
