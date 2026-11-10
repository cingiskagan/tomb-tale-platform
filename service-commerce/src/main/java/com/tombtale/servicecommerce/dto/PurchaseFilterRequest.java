package com.tombtale.servicecommerce.dto;

import com.tombtale.servicecommerce.domain.PurchaseStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

/**
 * Query-parameter DTO used to filter the purchase list endpoint.
 *
 * <p>All fields are optional — only non-null values are applied as
 * QueryDSL predicates. Date-time values must be provided in
 * ISO-8601 format (e.g. {@code 2026-01-15T10:30:00Z}).
 *
 * <p>{@code createdAfter} and {@code createdBefore} bound the entity's
 * {@code createdAt}, the platform-wide creation timestamp (ADR 0017).
 *
 * @param playerId      filter by the buyer's publicId (exact match)
 * @param itemCode      filter by catalogue item code (exact match)
 * @param status        filter by lifecycle state
 * @param createdAfter  include purchases on or after this instant
 * @param createdBefore include purchases on or before this instant
 */
public record PurchaseFilterRequest(
        UUID playerId,

        String itemCode,

        PurchaseStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdAfter,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdBefore
) {
}
