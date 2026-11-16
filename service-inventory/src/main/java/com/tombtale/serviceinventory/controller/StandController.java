package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.dto.StandEntryRequest;
import com.tombtale.serviceinventory.dto.StandEntryResponse;
import com.tombtale.serviceinventory.dto.StandResponse;
import com.tombtale.serviceinventory.service.StandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Dealers' stands, by the dealer's code, because the Unity town knows only that (ADR 0032).
 * Any caller reads a stand, and only {@code platform_admin} changes one.
 */
@RestController
@RequestMapping("/api/v1/inventory/stands/{dealerCode}")
@RequiredArgsConstructor
public class StandController {

    private final StandService standService;

    /**
     * An unknown dealer answers 404.
     *
     * @param dealerCode the dealer's code
     * @return what the dealer sells, with prices
     */
    @GetMapping
    @PreAuthorize(RoleConstants.IS_AUTHENTICATED)
    public StandResponse findStand(@PathVariable String dealerCode) {
        return standService.findStand(dealerCode);
    }

    /**
     * A bonus answers 403 while custom stand items are off, and a retired template 409.
     *
     * @param dealerCode the dealer's code
     * @param request    the template, the rarity and, for a custom entry, its bonus
     * @return the saved entry
     */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.IS_ADMIN)
    public StandEntryResponse addEntry(@PathVariable String dealerCode, @Valid @RequestBody StandEntryRequest request) {
        return standService.addEntry(dealerCode, request);
    }

    /**
     * @param dealerCode the dealer's code
     * @param entryId    the entry to take off its stand
     */
    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.IS_ADMIN)
    public void removeEntry(@PathVariable String dealerCode, @PathVariable UUID entryId) {
        standService.removeEntry(dealerCode, entryId);
    }
}
