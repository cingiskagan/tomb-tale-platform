package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.Rarity;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * A new entry on a stand. A bonus makes it a custom entry, which needs {@code inventory.custom-stand-items.enabled}.
 *
 * @param templateId the template it sells
 * @param rarity     the rarity of every copy
 * @param bonus      stat names with the values every copy carries, or null for copies that roll their own
 */
public record StandEntryRequest(
        @NotNull(message = "templateId is required")
        UUID templateId,

        @NotNull(message = "rarity is required")
        Rarity rarity,

        Map<String, @NotNull(message = "a stat needs a value") Integer> bonus
) {
}
