package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.InventorySettingsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The settings the portal hides its buttons by, for {@code platform_admin} only.
 */
@RestController
@RequestMapping("/api/v1/inventory/settings")
@PreAuthorize(RoleConstants.IS_ADMIN)
public class InventorySettingsController {

    private final InventorySettingsResponse settings;

    /** Reads the same keys as the services that enforce them. */
    public InventorySettingsController(
            @Value("${inventory.custom-stand-items.enabled}") boolean customStandItemsEnabled) {
        this.settings = new InventorySettingsResponse(customStandItemsEnabled);
    }

    /**
     * @return the settings as the service runs with them
     */
    @GetMapping
    public InventorySettingsResponse findSettings() {
        return settings;
    }
}
