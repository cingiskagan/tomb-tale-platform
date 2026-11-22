package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.Rarity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * A new entry on a stand. A bonus makes it a custom entry, which needs {@code inventory.custom-stand-items.enabled}.
 *
 * @param templateId the template it sells
 * @param rarity     the rarity of every copy
 * @param bonus      the lines every copy carries, as many as the rarity rolls and a stat more than once if need be,
 *                   or null for copies that roll their own
 */
public record StandEntryRequest(
        @NotNull(message = "templateId is required")
        UUID templateId,

        @NotNull(message = "rarity is required")
        Rarity rarity,

        List<@NotNull(message = "a bonus line is required") @Valid BonusLineRequest> bonus
) {
}
