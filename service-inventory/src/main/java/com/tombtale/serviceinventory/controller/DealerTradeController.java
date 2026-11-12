package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.DealerBuyRequest;
import com.tombtale.serviceinventory.dto.DealerSellRequest;
import com.tombtale.serviceinventory.security.InventoryCaller;
import com.tombtale.serviceinventory.service.DealerTradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A character's trades with dealers, with the same callers as its inventory (ADR 0031). A trade answers with the
 * inventory after it, and a trade id sent again answers with the inventory as it is.
 */
@RestController
@RequestMapping("/api/v1/inventory/characters/{characterId}")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_AUTHENTICATED)
public class DealerTradeController {

    private final DealerTradeService dealerTradeService;

    /**
     * A dungeon lock, too few coins or a changed price answers 409, and an entry no stand shows 404.
     *
     * @param characterId the character
     * @param request     the trade id, the stand entry and the price the player saw
     * @param token       the caller's token
     * @return the inventory after the buy
     */
    @PostMapping("/dealer-buys")
    public CharacterInventoryResponse buy(
            @PathVariable UUID characterId, @Valid @RequestBody DealerBuyRequest request, JwtAuthenticationToken token) {
        return dealerTradeService.buy(characterId, request, InventoryCaller.from(token));
    }

    /**
     * Every dealer pays the same share, so a sell names none. A dungeon lock or an equipped item answers 409.
     *
     * @param characterId the character
     * @param request     the trade id and the bag item
     * @param token       the caller's token
     * @return the inventory after the sell
     */
    @PostMapping("/dealer-sells")
    public CharacterInventoryResponse sell(
            @PathVariable UUID characterId, @Valid @RequestBody DealerSellRequest request, JwtAuthenticationToken token) {
        return dealerTradeService.sell(characterId, request, InventoryCaller.from(token));
    }
}
