package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Every field of a dealer that may change. The code never does.
 *
 * @param name    the name players see
 * @param version the version the edit started from. A stale one answers 409
 */
public record UpdateDealerRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name is at most 100 characters")
        String name,

        @NotNull(message = "version is required")
        Integer version
) {
}
