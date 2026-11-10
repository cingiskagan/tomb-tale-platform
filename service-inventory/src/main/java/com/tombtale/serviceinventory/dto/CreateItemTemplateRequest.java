package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * A new template. Inventory checks the stat names, and sets the stack size from the type.
 *
 * @param code      the stable key, upper case with underscores
 * @param name      the name players see
 * @param itemType  what kind of item it is, fixed from now on
 * @param price     what a gray copy costs, in coins
 * @param baseStats stat names with their values
 * @param assetKey  the Addressables address of its asset
 * @param iconHash  the hash of its uploaded icon, or null
 * @param itemSetId the set it belongs to, or null
 */
public record CreateItemTemplateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code is at most 100 characters")
        @Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "code is upper case letters, digits and underscores")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name,

        @NotNull(message = "itemType is required")
        ItemType itemType,

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

        UUID itemSetId
) {
}
