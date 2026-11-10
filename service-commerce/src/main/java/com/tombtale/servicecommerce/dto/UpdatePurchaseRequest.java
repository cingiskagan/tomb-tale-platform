package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.domain.PurchaseStatus;

/**
 * Inbound DTO for changing the status of an existing purchase. The quantity is fixed,
 * because inventory already granted it.
 *
 * @param status new lifecycle state, or null to change nothing
 */
public record UpdatePurchaseRequest(
        PurchaseStatus status
) {
}
