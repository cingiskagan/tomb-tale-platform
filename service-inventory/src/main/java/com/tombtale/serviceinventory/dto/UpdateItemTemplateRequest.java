package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Every field of a template that may change. The code and the type never do.
 *
 * @param name      the name players see
 * @param price     what a gray copy costs, in coins
 * @param baseStats stat names with their values
 * @param assetKey  the Addressables address of its asset
 * @param iconHash  the hash of its uploaded icon, or null
 * @param itemSetId the set it belongs to, or null
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

        @Pattern(regexp = "[0-9a-f]{64}", message = "iconHash is a SHA-256 in lower-case hex")
        String iconHash,

        UUID itemSetId,

        @NotNull(message = "version is required")
        Integer version
) {
}
