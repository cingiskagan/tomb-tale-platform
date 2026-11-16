package com.tombtale.serviceinventory.dto;

import java.util.List;

/**
 * A dealer's stand as the portal and the game show it, priced at read time.
 *
 * @param code    the dealer's code
 * @param name    the dealer's name
 * @param entries what it sells, oldest entry first. A retired dealer sells nothing
 */
public record StandResponse(
        String code,
        String name,
        List<StandEntryResponse> entries
) {
}
