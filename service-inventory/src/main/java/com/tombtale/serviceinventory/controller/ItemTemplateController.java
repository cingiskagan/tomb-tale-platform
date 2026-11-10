package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.serviceinventory.dto.CreateItemTemplateRequest;
import com.tombtale.serviceinventory.dto.ItemTemplateResponse;
import com.tombtale.serviceinventory.dto.UpdateItemTemplateRequest;
import com.tombtale.serviceinventory.service.ItemTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * The item catalog, for {@code platform_admin} only. A template is retired, never deleted.
 */
@RestController
@RequestMapping("/api/v1/inventory/templates")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.IS_ADMIN)
@Tag(name = "Item templates", description = "The item catalog")
@SecurityRequirement(name = "bearer-jwt")
public class ItemTemplateController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ItemTemplateService itemTemplateService;

    /**
     * @param request the new template
     * @return the saved template
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a template", description = "A taken code answers 409.")
    public ItemTemplateResponse createTemplate(@Valid @RequestBody CreateItemTemplateRequest request) {
        return itemTemplateService.createTemplate(request);
    }

    /**
     * @param pageable the page, by code unless the caller sorts
     * @return one page of templates, retired ones too
     */
    @GetMapping
    @Operation(summary = "List templates", description = "Retired templates are included.")
    public PagedResponse<ItemTemplateResponse> listTemplates(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "code", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PagedResponse.from(itemTemplateService.listTemplates(pageable));
    }

    /**
     * @param publicId the template
     * @return the template
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get a template")
    public ItemTemplateResponse findTemplate(@PathVariable UUID publicId) {
        return itemTemplateService.findTemplate(publicId);
    }

    /**
     * @param publicId the template
     * @param request  its new fields and the version the edit started from
     * @return the template after the edit
     */
    @PutMapping("/{publicId}")
    @Operation(summary = "Edit a template", description = "A stale version answers 409.")
    public ItemTemplateResponse updateTemplate(
            @PathVariable UUID publicId, @Valid @RequestBody UpdateItemTemplateRequest request) {
        return itemTemplateService.updateTemplate(publicId, request);
    }

    /**
     * @param publicId the template
     * @return the retired template
     */
    @PostMapping("/{publicId}/retire")
    @Operation(summary = "Retire a template", description = "It is never sold or granted again.")
    public ItemTemplateResponse retireTemplate(@PathVariable UUID publicId) {
        return itemTemplateService.retireTemplate(publicId);
    }
}
