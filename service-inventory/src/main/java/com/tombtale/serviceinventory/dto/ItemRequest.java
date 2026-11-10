package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Names the item to put in the slot or cell the path names.
 *
 * @param itemId the item's {@code publicId}
 */
public record ItemRequest(
        @NotNull(message = "itemId is required")
        UUID itemId
) {
}
