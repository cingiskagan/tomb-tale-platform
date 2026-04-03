package com.tombtale.servicecommerce.service;

import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.dto.UpdatePurchaseRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.exception.InvalidStatusTransitionException;
import com.tombtale.servicecommerce.exception.PurchaseNotFoundException;
import com.tombtale.servicecommerce.mapper.PurchaseMapper;
import com.tombtale.servicecommerce.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Business-logic layer for purchase lifecycle operations.
 *
 * <p>All mutating methods are transactional. Read-only methods default
 * to {@code readOnly = true} for Hibernate flush-mode optimisation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;

    /**
     * Creates a new purchase with status {@code PENDING}.
     *
     * <p>The {@code totalPrice} is computed as {@code quantity × unitPrice}
     * to guarantee ledger consistency regardless of client-supplied values.
     *
     * @param request the validated creation request
     * @return the persisted purchase as a response DTO
     */
    @Transactional
    public PurchaseResponse createPurchase(CreatePurchaseRequest request) {
        Purchase purchase = purchaseMapper.toEntity(request);
        purchase.setTotalPrice(calculateTotalPrice(request.unitPrice(), request.quantity()));
        purchase.setStatus(PurchaseStatus.PENDING);

        Purchase saved = purchaseRepository.save(purchase);
        LOG.info("Created purchase id={} for player={}", saved.getId(), saved.getPlayerId());
        return purchaseMapper.toResponse(saved);
    }

    /**
     * Retrieves a single purchase by its unique identifier.
     *
     * @param purchaseId the purchase UUID
     * @return the matching purchase DTO
     * @throws PurchaseNotFoundException if no purchase exists with the given ID
     */
    public PurchaseResponse findPurchaseById(UUID purchaseId) {
        Purchase purchase = findEntityById(purchaseId);
        return purchaseMapper.toResponse(purchase);
    }

    /**
     * Returns a paginated, filtered list of purchases.
     *
     * <p>Delegates dynamic predicate construction to the QueryDSL
     * repository fragment.
     *
     * @param filter   optional filter criteria (all fields nullable)
     * @param pageable pagination and sorting parameters
     * @return a page of matching purchase DTOs
     */
    public Page<PurchaseResponse> listPurchases(PurchaseFilterRequest filter, Pageable pageable) {
        Page<Purchase> page = purchaseRepository.findByFilter(filter, pageable);
        return page.map(purchaseMapper::toResponse);
    }

    /**
     * Applies a partial update to an existing purchase.
     *
     * <p>Only non-null fields in the request are written. When
     * {@code quantity} changes, {@code totalPrice} is recalculated
     * from the existing {@code unitPrice}.
     *
     * @param purchaseId the purchase UUID to update
     * @param request    the partial-update payload
     * @return the updated purchase DTO
     * @throws PurchaseNotFoundException if the purchase does not exist
     */
    @Transactional
    public PurchaseResponse updatePurchase(UUID purchaseId, UpdatePurchaseRequest request) {
        Purchase purchase = findEntityById(purchaseId);

        if (request.status() != null) {
            if (request.status() == PurchaseStatus.CANCELLED) {
                throw new InvalidStatusTransitionException(
                        "Use DELETE endpoint to cancel a purchase");
            }
            purchase.setStatus(request.status());
        }
        if (request.quantity() != null) {
            purchase.setQuantity(request.quantity());
            purchase.setTotalPrice(calculateTotalPrice(purchase.getUnitPrice(), request.quantity()));
        }

        Purchase saved = purchaseRepository.save(purchase);
        LOG.info("Updated purchase id={}", saved.getId());
        return purchaseMapper.toResponse(saved);
    }

    /**
     * Soft-deletes a purchase by setting its status to {@code CANCELLED}.
     *
     * <p>The row is not physically removed — it is simply excluded from
     * default list queries by the QueryDSL filter.
     *
     * @param purchaseId the purchase UUID to cancel
     * @throws PurchaseNotFoundException if the purchase does not exist
     */
    @Transactional
    public void deletePurchase(UUID purchaseId) {
        Purchase purchase = findEntityById(purchaseId);
        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchaseRepository.save(purchase);
        LOG.info("Soft-deleted (cancelled) purchase id={}", purchaseId);
    }

    /**
     * Internal helper that loads a {@link Purchase} or throws.
     *
     * @param purchaseId the purchase UUID
     * @return the managed entity
     */
    private Purchase findEntityById(UUID purchaseId) {
        return purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new PurchaseNotFoundException(purchaseId));
    }

    /**
     * Computes total price as {@code unitPrice × quantity}.
     *
     * @param unitPrice the price per item
     * @param quantity  the number of items
     * @return the total price
     */
    private static BigDecimal calculateTotalPrice(BigDecimal unitPrice, Integer quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
