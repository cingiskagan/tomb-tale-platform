package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.ItemRequest;
import com.tombtale.serviceinventory.security.InventoryCaller;
import com.tombtale.serviceinventory.service.ItemPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A character's equipment slots, with the same callers as its inventory. Each change answers with the inventory
 * after it, so a client redraws from the answer.
 */
@RestController
@RequestMapping("/api/v1/inventory/characters/{characterId}/equipment")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_AUTHENTICATED)
public class EquipmentController {

    private final ItemPlacementService itemPlacementService;

    /**
     * The items it replaces go to the bag.
     *
     * @param characterId the character
     * @param slot        the slot to equip
     * @param request     the bag item to put in it
     * @param token       the caller's token
     * @return the inventory after the change
     */
    @PutMapping("/{slot}")
    public CharacterInventoryResponse equip(
            @PathVariable UUID characterId,
            @PathVariable EquipmentSlot slot,
            @Valid @RequestBody ItemRequest request,
            JwtAuthenticationToken token) {
        return itemPlacementService.equip(characterId, slot, request.itemId(), InventoryCaller.from(token));
    }

    /**
     * @param characterId the character
     * @param slot        the slot to empty into the lowest free cell
     * @param token       the caller's token
     * @return the inventory after the change
     */
    @DeleteMapping("/{slot}")
    public CharacterInventoryResponse unequip(
            @PathVariable UUID characterId, @PathVariable EquipmentSlot slot, JwtAuthenticationToken token) {
        return itemPlacementService.unequip(characterId, slot, InventoryCaller.from(token));
    }
}
