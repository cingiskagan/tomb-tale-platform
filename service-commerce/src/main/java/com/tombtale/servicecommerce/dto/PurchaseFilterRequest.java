package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.entity.PurchaseStatus;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Query-parameter DTO used to filter the purchase list endpoint.
 *
 * <p>All fields are optional — only non-null values are applied as
 * QueryDSL predicates. Date-time values must be provided in
 * ISO-8601 format (e.g. {@code 2026-01-15T10:30:00Z}).
 *
 * @param playerId       filter by player identifier (exact match)
 * @param itemCode       filter by catalogue item code (exact match)
 * @param status         filter by lifecycle state
 * @param purchasedAfter include purchases on or after this instant
 * @param purchasedBefore include purchases on or before this instant
 */
public record PurchaseFilterRequest(
        @Schema(example = "player-001", description = "Target player identifier")
        String playerId,
        
        @Schema(example = "SWORD_IRON", description = "Specific catalogue item code")
        String itemCode,
        
        @Schema(example = "PENDING", description = "Current status of the purchase")
        PurchaseStatus status,
        
        @Schema(example = "2026-01-01T00:00:00Z", description = "Include purchases on or after this instant")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
        Instant purchasedAfter,
        
        @Schema(example = "2026-12-31T23:59:59Z", description = "Include purchases on or before this instant")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
        Instant purchasedBefore
) {
}
