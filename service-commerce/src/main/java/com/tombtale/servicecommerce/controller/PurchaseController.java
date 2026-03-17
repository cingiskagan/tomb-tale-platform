package com.tombtale.servicecommerce.controller;

import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseOrderDto;
import com.tombtale.servicecommerce.entity.PurchaseOrder;
import com.tombtale.servicecommerce.service.PurchaseService;
import com.querydsl.core.types.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for purchase order operations.
 * <p>
 * All endpoints require a valid Zitadel JWT. The authenticated player
 * is identified via the "sub" claim in the token.
 */
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * POST /api/v1/purchases
     * <p>
     * Creates a new purchase order. If the idempotency key already
     * exists, returns the existing order (at-most-once guarantee).
     */
    @PostMapping
    public ResponseEntity<PurchaseOrderDto> createPurchase(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePurchaseRequest request) {

        String zitadelUserId = jwt.getSubject();
        PurchaseOrderDto result = purchaseService.createPurchase(zitadelUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/v1/purchases
     * <p>
     * Lists the current player's purchases with optional QueryDSL
     * filters (e.g., ?status=COMPLETED&amp;currency=GOLD) and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<PurchaseOrderDto>> listPurchases(
            @QuerydslPredicate(root = PurchaseOrder.class) Predicate predicate,
            Pageable pageable) {

        Page<PurchaseOrderDto> results = purchaseService.findPurchases(predicate, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/v1/purchases/{id}
     * <p>
     * Returns a specific purchase order by its UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDto> getPurchaseById(@PathVariable UUID id) {
        PurchaseOrderDto result = purchaseService.findPurchaseById(id);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/purchases/{id}/cancel
     * <p>
     * Cancels a pending purchase order. Only the order owner can cancel.
     * Orders not in PENDING status will be rejected with 409 Conflict.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancelPurchase(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        String zitadelUserId = jwt.getSubject();
        PurchaseOrderDto result = purchaseService.cancelPurchase(zitadelUserId, id);
        return ResponseEntity.ok(result);
    }
}
