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
 * @param minLevel    include players at or above this level
 * @param maxLevel    include players at or below this level
 */
public record PlayerFilterRequest(
        @Schema(example = "Dark", description = "Partial display name match (case-insensitive)") String displayName,

        @Schema(example = "10", description = "Minimum player level (inclusive)") Integer minLevel,

        @Schema(example = "50", description = "Maximum player level (inclusive)") Integer maxLevel) {
}
