package com.tombtale.serviceinventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Every field of a template that may change. The code and the type never do.
 *
 * @param name      the name players see
 * @param price     what a gray copy costs, in coins
 * @param baseStats stat names with their values
 * @param assetKey  the Addressables address of its asset
 * @param version   the version the edit started from. A stale one answers 409
 */
public record UpdateItemTemplateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must not be negative")
        Long price,

        @NotNull(message = "baseStats is required")
        Map<String, @NotNull(message = "a stat needs a value") Integer> baseStats,

        @NotBlank(message = "assetKey is required")
        @Size(max = 200, message = "assetKey is at most 200 characters")
        String assetKey,

        @Schema(example = "0")
        @NotNull(message = "version is required")
        Integer version
) {
}
