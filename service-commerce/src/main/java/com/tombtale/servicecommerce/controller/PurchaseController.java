package com.tombtale.servicecommerce.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing CRUD operations for in-game purchases.
 *
 * <p>All endpoints are currently public (no JWT required) to support
 * early development and Swagger UI testing. Authentication will be
 * enforced once the frontend integrates Zitadel.
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
     * @param request the validated creation payload
     * @return the created purchase with generated ID and computed totalPrice
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new purchase",
            description = "Creates a PENDING purchase and calculates totalPrice server-side."
    )
    public PurchaseResponse createPurchase(@Valid @RequestBody CreatePurchaseRequest request) {
        return purchaseService.createPurchase(request);
    }

    /**
     * Retrieves a single purchase by its UUID.
     *
     * @param id the purchase identifier
     * @return the matching purchase
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get purchase by ID")
    public PurchaseResponse findPurchaseById(
            @Parameter(description = "Purchase UUID") @PathVariable UUID id) {
        return purchaseService.findPurchaseById(id);
    }

    /**
     * Lists purchases with optional filtering and pagination.
     *
     * <p>Soft-deleted ({@code CANCELLED}) purchases are excluded unless
     * explicitly filtered by status.
     *
     * @param filter   optional query-parameter filters
     * @param pageable pagination controls (default page size: 20)
     * @return a page of matching purchases
     */
    @GetMapping
    @Operation(
            summary = "List purchases",
            description = "Paginated list with optional filters. CANCELLED purchases hidden by default."
    )
    public Page<PurchaseResponse> listPurchases(
            @ModelAttribute PurchaseFilterRequest filter,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return purchaseService.listPurchases(filter, pageable);
    }

    /**
     * Partially updates an existing purchase.
     *
     * @param id      the purchase identifier
     * @param request the partial-update payload
     * @return the updated purchase
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a purchase", description = "Partial update — only non-null fields are applied.")
    public PurchaseResponse updatePurchase(
            @Parameter(description = "Purchase UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseRequest request) {
        return purchaseService.updatePurchase(id, request);
    }

    /**
     * Soft-deletes a purchase by setting its status to {@code CANCELLED}.
     *
     * @param id the purchase identifier
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Soft-delete a purchase",
            description = "Sets status to CANCELLED — row is preserved for audit."
    )
    public void deletePurchase(
            @Parameter(description = "Purchase UUID") @PathVariable UUID id) {
        purchaseService.deletePurchase(id);
    }
}
