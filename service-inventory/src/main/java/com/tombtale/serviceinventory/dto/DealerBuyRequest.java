package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * A buy from a dealer's stand. A second buy with the same trade id changes nothing.
 *
 * @param tradeId a new id the client makes for each buy
 * @param entryId the stand entry to buy a copy of
 * @param price   the price the player saw. If the stand's price changed since, the buy answers 409
 */
public record DealerBuyRequest(
        @NotNull(message = "tradeId is required")
        UUID tradeId,

        @NotNull(message = "entryId is required")
        UUID entryId,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must not be negative")
        Long price
) {
}
