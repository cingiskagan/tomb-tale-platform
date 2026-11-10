package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.security.InventoryCaller;
import com.tombtale.serviceinventory.service.CharacterInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A character's inventory, for {@code platform_admin} on any character and for a player on their own (ADR 0031).
 */
@RestController
@RequestMapping("/api/v1/inventory/characters")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_AUTHENTICATED)
public class CharacterInventoryController {

    private final CharacterInventoryService characterInventoryService;

    /**
     * Another player's character answers 403, and one inventory doesn't know 404.
     *
     * @param characterId the character
     * @param token       the caller's token
     * @return its coins, equipment and bag
     */
    @GetMapping("/{characterId}")
    public CharacterInventoryResponse findInventory(@PathVariable UUID characterId, JwtAuthenticationToken token) {
        return characterInventoryService.findInventory(characterId, InventoryCaller.from(token));
    }
}
