package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A sell to a dealer. Every dealer pays the same, so it names none, and a second sell with the same trade id
 * changes nothing.
 *
 * @param tradeId a new id the client makes for each sell
 * @param itemId  the bag item to sell, with every copy in its stack
 */
public record DealerSellRequest(
        @NotNull(message = "tradeId is required")
        UUID tradeId,

        @NotNull(message = "itemId is required")
        UUID itemId
) {
}
