package com.tombtale.serviceplayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Query-parameter DTO for filtering the player list endpoint.
 *
 * <p>
 * All fields are optional — only non-null values are applied as
 * QueryDSL predicates.
 *
 * @param displayName partial match on display name (case-insensitive)
 */
public record PlayerFilterRequest(
        @Schema(example = "Dark", description = "Partial display name match (case-insensitive)") String displayName) {
}
