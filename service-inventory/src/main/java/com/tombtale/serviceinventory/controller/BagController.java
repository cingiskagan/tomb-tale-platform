package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.ItemRequest;
import com.tombtale.serviceinventory.security.InventoryCaller;
import com.tombtale.serviceinventory.service.ItemPlacementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A character's bag cells, with the same callers as its inventory. A move answers with the inventory after it.
 */
@RestController
@RequestMapping("/api/v1/inventory/characters/{characterId}/bag")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_AUTHENTICATED)
public class BagController {

    private final ItemPlacementService itemPlacementService;

    /**
     * A taken cell swaps the two items.
     *
     * @param characterId the character
     * @param cell        the cell to move to, from 0
     * @param request     the bag item to move
     * @param token       the caller's token
     * @return the inventory after the move
     */
    @PutMapping("/{cell}")
    public CharacterInventoryResponse move(
            @PathVariable UUID characterId,
            @PathVariable @PositiveOrZero int cell,
            @Valid @RequestBody ItemRequest request,
            JwtAuthenticationToken token) {
        return itemPlacementService.move(characterId, cell, request.itemId(), InventoryCaller.from(token));
    }
}
