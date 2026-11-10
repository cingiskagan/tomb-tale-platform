package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Every field of a set that may change. The code never does.
 *
 * @param name    the name players see
 * @param bonus   stat names with the values the complete set adds
 * @param version the version the edit started from. A stale one answers 409
 */
public record UpdateItemSetRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name,

        @NotNull(message = "bonus is required")
        Map<String, @NotNull(message = "a stat needs a value") Integer> bonus,

        @NotNull(message = "version is required")
        Integer version
) {
}
