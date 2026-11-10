package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * A new item set. Inventory checks the stat names of its bonus.
 *
 * @param code  the stable key, upper case with underscores
 * @param name  the name players see
 * @param bonus stat names with the values the complete set adds
 */
public record CreateItemSetRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code is at most 100 characters")
        @Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "code is upper case letters, digits and underscores")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name,

        @NotNull(message = "bonus is required")
        Map<String, @NotNull(message = "a stat needs a value") Integer> bonus
) {
}
