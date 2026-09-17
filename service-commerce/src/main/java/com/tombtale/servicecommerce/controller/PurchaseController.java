package com.tombtale.servicecommerce.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.commons.web.PagedResponse;
import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.dto.UpdatePurchaseRequest;
import com.tombtale.servicecommerce.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing CRUD operations for in-game purchases.
 *
 * <p>
 * Endpoints are documented with bearer JWT security in OpenAPI. Runtime
 * access control is enforced per method by {@code @PreAuthorize}: mutations
 * require {@code platform_admin}, reads also accept {@code game_master}, and
 * {@code player} has no access. Authorities come from Zitadel project roles
 * (see {@code ZitadelRoleConverter}).
 */
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@Tag(name = "Purchases", description = "In-game purchase CRUD operations")
@SecurityRequirement(name = "bearer-jwt")
public class PurchaseController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PurchaseService purchaseService;

    /**
     * Creates a new purchase order.
     *
     * <p>
     * Requires {@code platform_admin}. The buyer comes from
     * {@code request.playerId()}, not from the token subject — an admin
     * creates purchases on behalf of a player. That field is the player's
     * {@code publicId} (ADR 0014).
     *
     * @param request the validated creation payload
     * @return the created purchase with generated publicId and computed totalPrice
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(RoleConstants.IS_ADMIN)
    @Operation(summary = "Create a new purchase", description = "Creates a PENDING purchase and calculates totalPrice server-side.")
    public PurchaseResponse createPurchase(@Valid @RequestBody CreatePurchaseRequest request) {
        return purchaseService.createPurchase(request);
    }

    /**
     * Retrieves a single purchase by its UUID.
     *
     * <p>
     * Requires {@code platform_admin} or {@code game_master}.
     *
     * @param publicId the purchase's public identifier
     * @return the matching purchase
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get purchase by publicId")
    @PreAuthorize(RoleConstants.IS_ADMIN_OR_GAME_MASTER)
    public PurchaseResponse findPurchaseByPublicId(
            @Parameter(description = "Purchase UUID") @PathVariable UUID publicId) {
        return purchaseService.findPurchaseByPublicId(publicId);
    }

    /**
     * Lists purchases with optional filtering and pagination.
     *
     * <p>
     * Soft-deleted ({@code CANCELLED}) purchases are excluded unless
     * explicitly filtered by status.
     *
     * <p>
     * Requires {@code platform_admin} or {@code game_master}. This returns
     * every player's purchases, which is why {@code player} is excluded.
     *
     * @param filter   optional query-parameter filters
     * @param pageable pagination controls (default page size: 20)
     * @return one page of matching purchases in the platform's envelope
     */
    @GetMapping
    @PreAuthorize(RoleConstants.IS_ADMIN_OR_GAME_MASTER)
    @Operation(summary = "List purchases", description = "Paginated list with optional filters. CANCELLED purchases hidden by default.")
    public PagedResponse<PurchaseResponse> listPurchases(
            @ModelAttribute PurchaseFilterRequest filter,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PagedResponse.from(purchaseService.listPurchases(filter, pageable));
    }

    /**
     * Partially updates an existing purchase.
     *
     * <p>
     * PATCH, not PUT: the request body carries only the fields to change and
     * everything absent from it is left alone, which is what PATCH means.
     *
     * <p>
     * Requires {@code platform_admin}.
     *
     * @param publicId the purchase's public identifier
     * @param request  the partial-update payload
     * @return the updated purchase
     */
    @PatchMapping("/{publicId}")
    @PreAuthorize(RoleConstants.IS_ADMIN)
    @Operation(summary = "Update a purchase", description = "Partial update — only non-null fields are applied.")
    public PurchaseResponse updatePurchase(
            @Parameter(description = "Purchase UUID") @PathVariable UUID publicId,
            @Valid @RequestBody UpdatePurchaseRequest request) {
        return purchaseService.updatePurchase(publicId, request);
    }

    /**
     * Soft-deletes a purchase by setting its status to {@code CANCELLED}.
     *
     * <p>
     * Requires {@code platform_admin}.
     *
     * @param publicId the purchase's public identifier
     */
    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(RoleConstants.IS_ADMIN)
    @Operation(summary = "Soft-delete a purchase", description = "Sets status to CANCELLED — row is preserved for audit.")
    public void deletePurchase(
            @Parameter(description = "Purchase UUID") @PathVariable UUID publicId) {
        purchaseService.deletePurchase(publicId);
    }
}
