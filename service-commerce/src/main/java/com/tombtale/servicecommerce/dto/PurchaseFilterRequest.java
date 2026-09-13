package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.domain.PurchaseStatus;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Query-parameter DTO used to filter the purchase list endpoint.
 *
 * <p>All fields are optional — only non-null values are applied as
 * QueryDSL predicates. Date-time values must be provided in
 * ISO-8601 format (e.g. {@code 2026-01-15T10:30:00Z}).
 *
 * <p>{@code purchasedAfter} and {@code purchasedBefore} bound the entity's
 * {@code createdAt}. The API keeps the purchase-flavoured names; the column
 * behind them is the platform-wide creation timestamp.
 *
 * @param playerId       filter by the buyer's publicId (exact match)
 * @param itemCode       filter by catalogue item code (exact match)
 * @param status         filter by lifecycle state
 * @param purchasedAfter include purchases on or after this instant
 * @param purchasedBefore include purchases on or before this instant
 */
public record PurchaseFilterRequest(
        @Schema(example = "aaaaaaaa-0000-4000-8000-000000000001", description = "publicId of the target player")
        UUID playerId,
        
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
