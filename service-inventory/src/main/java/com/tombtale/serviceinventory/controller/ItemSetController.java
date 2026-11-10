package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.serviceinventory.dto.CreateItemSetRequest;
import com.tombtale.serviceinventory.dto.ItemSetResponse;
import com.tombtale.serviceinventory.dto.UpdateItemSetRequest;
import com.tombtale.serviceinventory.service.ItemSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Item sets, for {@code platform_admin} only.
 */
@RestController
@RequestMapping("/api/v1/inventory/sets")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_ADMIN)
public class ItemSetController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ItemSetService itemSetService;

    /**
     * A taken code answers 409.
     *
     * @param request the new set
     * @return the saved set
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemSetResponse createSet(@Valid @RequestBody CreateItemSetRequest request) {
        return itemSetService.createSet(request);
    }

    /**
     * @param pageable the page, by code unless the caller sorts
     * @return one page of sets
     */
    @GetMapping
    public PagedResponse<ItemSetResponse> listSets(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PagedResponse.from(itemSetService.listSets(pageable));
    }

    /**
     * @param publicId the set
     * @return the set
     */
    @GetMapping("/{publicId}")
    public ItemSetResponse findSet(@PathVariable UUID publicId) {
        return itemSetService.findSet(publicId);
    }

    /**
     * A stale version answers 409.
     *
     * @param publicId the set
     * @param request  its new fields and the version the edit started from
     * @return the set after the edit
     */
    @PutMapping("/{publicId}")
    public ItemSetResponse updateSet(@PathVariable UUID publicId, @Valid @RequestBody UpdateItemSetRequest request) {
        return itemSetService.updateSet(publicId, request);
    }
}
