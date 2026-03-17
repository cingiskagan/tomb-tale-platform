package com.tombtale.servicecommerce.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.tombtale.servicecommerce.dto.CreatePurchaseRequest;
import com.tombtale.servicecommerce.dto.PurchaseOrderDto;
import com.tombtale.servicecommerce.entity.OrderStatus;
import com.tombtale.servicecommerce.entity.OutboxEvent;
import com.tombtale.servicecommerce.entity.PurchaseItem;
import com.tombtale.servicecommerce.entity.PurchaseOrder;
import com.tombtale.servicecommerce.exception.InvalidOrderStateException;
import com.tombtale.servicecommerce.exception.PurchaseNotFoundException;
import com.tombtale.servicecommerce.mapper.PurchaseOrderMapper;
import com.tombtale.servicecommerce.repository.OutboxEventRepository;
import com.tombtale.servicecommerce.repository.PurchaseOrderRepository;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for purchase order management.
 * <p>
 * Every write operation persists both the entity change and an
 * {@link OutboxEvent} in the same transaction — guaranteeing that
 * the RabbitMQ event is never lost even if the broker is down.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    /** Number of visible characters for masked IDs in log messages. */
    private static final int VISIBLE_ID_CHARS = 4;

    /** Minimum length to safely mask. */
    private static final int MIN_MASKABLE_ID_LENGTH = 8;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new purchase order or returns an existing one if the
     * idempotency key has already been used (at-most-once guarantee).
     *
     * @param zitadelUserId the authenticated player's Zitadel user ID
     * @param request       validated purchase creation request
     * @return the created (or existing) purchase order as a DTO
     */
    @Transactional
    public PurchaseOrderDto createPurchase(String zitadelUserId, CreatePurchaseRequest request) {
        log.debug("Creating purchase for user: {}", maskId(zitadelUserId));

        Optional<PurchaseOrder> existingOrder =
                purchaseOrderRepository.findByIdempotencyKey(request.idempotencyKey());

        if (existingOrder.isPresent()) {
            log.info("Idempotent duplicate detected for key: {}", request.idempotencyKey());
            return purchaseOrderMapper.toDto(existingOrder.get());
        }

        PurchaseOrder order = buildPurchaseOrder(zitadelUserId, request);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        persistOutboxEvent(savedOrder, "purchase.created");

        log.info("Purchase order created: {}", savedOrder.getId());
        return purchaseOrderMapper.toDto(savedOrder);
    }

    /**
     * Searches purchase orders for a given player with optional QueryDSL
     * filters (status, currency, date range).
     *
     * @param predicate QueryDSL predicate built from request parameters
     * @param pageable  pagination and sorting information
     * @return page of matching purchase order DTOs
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> findPurchases(Predicate predicate, Pageable pageable) {
        return purchaseOrderRepository.findAll(predicate, pageable)
                .map(purchaseOrderMapper::toDto);
    }

    /**
     * Retrieves a specific purchase order by ID.
     *
     * @param orderId the purchase order UUID
     * @return the purchase order DTO
     * @throws PurchaseNotFoundException if the order does not exist
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDto findPurchaseById(UUID orderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new PurchaseNotFoundException(
                        "Purchase order not found: " + orderId));
        return purchaseOrderMapper.toDto(order);
    }

    /**
     * Cancels a pending purchase order. Only orders in {@code PENDING}
     * status can be cancelled — completed or refunded orders will throw
     * an {@link InvalidOrderStateException}.
     * <p>
     * Uses optimistic locking to prevent concurrent status changes.
     *
     * @param zitadelUserId the authenticated player's Zitadel user ID
     * @param orderId       the purchase order UUID
     * @return the cancelled purchase order as a DTO
     */
    @Transactional
    public PurchaseOrderDto cancelPurchase(String zitadelUserId, UUID orderId) {
        log.debug("Cancelling purchase {} for user: {}", orderId, maskId(zitadelUserId));

        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new PurchaseNotFoundException(
                        "Purchase order not found: " + orderId));

        if (!order.getZitadelUserId().equals(zitadelUserId)) {
            throw new PurchaseNotFoundException("Purchase order not found: " + orderId);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        persistOutboxEvent(savedOrder, "purchase.cancelled");

        log.info("Purchase order cancelled: {}", orderId);
        return purchaseOrderMapper.toDto(savedOrder);
    }

    /**
     * Builds a {@link PurchaseOrder} entity from the creation request,
     * computing the total price from line items.
     */
    private PurchaseOrder buildPurchaseOrder(String zitadelUserId, CreatePurchaseRequest request) {
        PurchaseOrder order = PurchaseOrder.builder()
                .idempotencyKey(request.idempotencyKey())
                .zitadelUserId(zitadelUserId)
                .currency(request.currency())
                .status(OrderStatus.PENDING)
                .build();

        List<PurchaseItem> items = request.items().stream()
                .map(itemReq -> PurchaseItem.builder()
                        .order(order)
                        .itemCatalogId(itemReq.itemCatalogId())
                        .itemName(itemReq.itemName())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .build())
                .toList();

        order.setItems(items);

        BigDecimal totalPrice = items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(totalPrice);
        return order;
    }

    /**
     * Persists an outbox event in the same transaction as the order change.
     * The {@link OutboxPublisher} will later read and publish it to RabbitMQ.
     */
    private void persistOutboxEvent(PurchaseOrder order, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(
                    purchaseOrderMapper.toDto(order));

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(order.getId().toString())
                    .eventType(eventType)
                    .payload(payload)
                    .published(false)
                    .build();

            outboxEventRepository.save(event);
        } catch (JacksonException ex) {
            throw new IllegalStateException(
                    "Failed to serialise outbox event for order: " + order.getId(), ex);
        }
    }

    /**
     * Masks an external identifier for safe logging.
     * Keeps first and last characters visible for correlation.
     */
    private String maskId(String id) {
        if (id == null || id.length() <= MIN_MASKABLE_ID_LENGTH) {
            return "***";
        }
        return id.substring(0, VISIBLE_ID_CHARS) + "***"
                + id.substring(id.length() - VISIBLE_ID_CHARS);
    }
}
