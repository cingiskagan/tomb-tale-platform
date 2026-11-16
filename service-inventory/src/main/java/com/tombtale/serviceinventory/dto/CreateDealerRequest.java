package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new dealer, with an empty stand.
 *
 * @param code the stable key the Unity town finds it by, upper case with underscores
 * @param name the name players see
 */
public record CreateDealerRequest(
        @NotBlank(message = "code is required")
        @Size(max = 100, message = "code is at most 100 characters")
        @Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "code is upper case letters, digits and underscores")
        String code,

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name
) {
}
