package com.tombtale.serviceinventory.dto;

/**
 * The settings the portal follows, so it hides what inventory would refuse.
 *
 * @param customStandItemsEnabled whether a stand takes a custom entry, from {@code inventory.custom-stand-items.enabled}
 */
public record InventorySettingsResponse(
        boolean customStandItemsEnabled
) {
}
