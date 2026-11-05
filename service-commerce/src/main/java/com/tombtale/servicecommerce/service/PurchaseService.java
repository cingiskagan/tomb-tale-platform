package com.tombtale.servicecommerce.service;

import com.tombtale.servicecommerce.domain.PurchaseStatus;
import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.dto.PurchaseResponse;
import com.tombtale.servicecommerce.dto.UpdatePurchaseRequest;
import com.tombtale.servicecommerce.dto.event.PurchaseCreatedPayload;
import com.tombtale.servicecommerce.entity.Purchase;
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
 * <p>
 * All mutating methods are transactional. Read-only methods default
 * to {@code readOnly = true} for Hibernate flush-mode optimisation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseRepository purchaseRepository;
    private final PurchaseMapper purchaseMapper;
    private final OutboxService outboxService;

    /**
     * Creates a new purchase with status {@code PENDING}, and writes {@code purchase.created} for inventory to turn into an item grant.
     * The total is {@code quantity × unitPrice}, computed here and never taken from the client.
     *
     * @param request the validated creation request
     * @return the persisted purchase as a response DTO
     */
    @Transactional
    public PurchaseResponse createPurchase(CreatePurchaseRequest request) {
        Purchase purchase = purchaseMapper.toEntity(request);
        purchase.setTotalPrice(request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())));
        purchase.setStatus(PurchaseStatus.PENDING);

        Purchase saved = purchaseRepository.save(purchase);
        outboxService.append(PurchaseCreatedPayload.EVENT_TYPE, PurchaseCreatedPayload.EVENT_VERSION, saved.getPublicId(),
                new PurchaseCreatedPayload(saved.getPublicId(), saved.getPlayerId(), saved.getItemCode(), saved.getQuantity()));
        LOG.info("Created purchase id={} for player={}", saved.getPublicId(), saved.getPlayerId());
        return purchaseMapper.toResponse(saved);
    }

    /**
     * Completes a purchase once inventory granted its items. A repeated item grant finds it completed and changes nothing.
     *
     * @param publicId the purchase that inventory names in {@code item.granted}
     * @throws InvalidStatusTransitionException if the purchase is no longer pending
     */
    @Transactional
    public void completeGrantedPurchase(UUID publicId) {
        Purchase purchase = findEntityByPublicId(publicId);
        changeStatus(purchase, PurchaseStatus.COMPLETED);
        LOG.info("Completed purchase id={} after its grant", publicId);
    }

    /**
     * Cancels a purchase that inventory refused. A repeated rejection finds it cancelled and changes nothing.
     *
     * @param publicId the purchase that inventory names in {@code item.grant-rejected}
     * @param reason   why inventory refused, kept on the purchase as it arrived
     * @throws InvalidStatusTransitionException if the purchase is no longer pending
     */
    @Transactional
    public void cancelRejectedPurchase(UUID publicId, String reason) {
        Purchase purchase = findEntityByPublicId(publicId);
        changeStatus(purchase, PurchaseStatus.CANCELLED);
        purchase.setCancellationReason(reason);
        LOG.info("Cancelled purchase id={} after its failure: {}", publicId, reason);
    }

    /**
     * Retrieves a single purchase by the identifier its API exposes.
     *
     * @param publicId the purchase's public UUID
     * @return the matching purchase DTO
     * @throws PurchaseNotFoundException if no purchase carries that ID
     */
    public PurchaseResponse findPurchaseByPublicId(UUID publicId) {
        Purchase purchase = findEntityByPublicId(publicId);
        return purchaseMapper.toResponse(purchase);
    }

    /**
     * Returns a paginated, filtered list of purchases.
     *
     * <p>
     * Delegates dynamic predicate construction to the QueryDSL
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
     * Changes the status of a purchase, for example from {@code COMPLETED} to {@code REFUNDED}.
     * Only inventory's item grant completes a purchase, because a pending purchase is refused here.
     *
     * @param publicId the purchase's public UUID
     * @param request  the new status, or null to change nothing
     * @return the updated purchase DTO
     * @throws PurchaseNotFoundException if the purchase does not exist
     * @throws InvalidStatusTransitionException if the purchase is pending or the move is not allowed
     */
    @Transactional
    public PurchaseResponse updatePurchase(UUID publicId, UpdatePurchaseRequest request) {
        Purchase purchase = findEntityByPublicId(publicId);
        refusePending(purchase);

        if (request.status() != null
                && purchase.getStatus() != request.status()) {

            if (request.status() == PurchaseStatus.CANCELLED) {
                throw new InvalidStatusTransitionException(
                        "Use DELETE endpoint to cancel a purchase");
            }
            changeStatus(purchase, request.status());
        }

        Purchase saved = purchaseRepository.save(purchase);
        LOG.info("Updated purchase id={}", saved.getPublicId());
        return purchaseMapper.toResponse(saved);
    }

    private void changeStatus(Purchase purchase, PurchaseStatus target) {
        if (purchase.getStatus() == target) {
            return;
        }
        if (!purchase.getStatus().canTransitionTo(target)) {
            String msg = "Cannot change purchase status from %s to %s".formatted(purchase.getStatus(), target);
            throw new InvalidStatusTransitionException(msg);
        }
        purchase.setStatus(target);
    }

    /** Only inventory's item grant moves a purchase out of {@code PENDING} (ADR 0027), so an admin waits for it. */
    private static void refusePending(Purchase purchase) {
        if (purchase.getStatus() == PurchaseStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                    "Purchase %s stays PENDING until inventory sends its item grant".formatted(purchase.getPublicId()));
        }
    }

    /**
     * Soft-deletes a purchase by setting its status to {@code CANCELLED}.
     *
     * <p>
     * The row is not physically removed — it is simply excluded from
     * default list queries by the QueryDSL filter.
     *
     * @param publicId the purchase's public UUID
     * @throws PurchaseNotFoundException if the purchase does not exist
     * @throws InvalidStatusTransitionException if the purchase is pending
     */
    @Transactional
    public void deletePurchase(UUID publicId) {
        Purchase purchase = findEntityByPublicId(publicId);
        refusePending(purchase);
        changeStatus(purchase, PurchaseStatus.CANCELLED);
        purchaseRepository.save(purchase);
        LOG.info("Soft-deleted (cancelled) purchase id={}", publicId);
    }

    /**
     * Internal helper that loads a {@link Purchase} or throws.
     *
     * @param publicId the purchase's public UUID
     * @return the managed entity
     */
    private Purchase findEntityByPublicId(UUID publicId) {
        return purchaseRepository.findByPublicId(publicId)
                .orElseThrow(() -> new PurchaseNotFoundException(publicId));
    }
}
