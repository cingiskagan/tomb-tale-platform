package com.tombtale.serviceplayer.dto;

/**
 * Query-parameter DTO for filtering the player list endpoint.
 *
 * <p>
 * All fields are optional — only non-null values are applied as
 * QueryDSL predicates.
 *
 * @param displayName partial match on display name (case-insensitive)
 */
public record PlayerFilterRequest(String displayName) {
}
