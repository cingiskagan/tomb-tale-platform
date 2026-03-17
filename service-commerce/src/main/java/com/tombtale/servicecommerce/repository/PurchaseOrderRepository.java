package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link PurchaseOrder} entities.
 * <p>
 * Extends {@link QuerydslPredicateExecutor} to support type-safe
 * dynamic queries via QueryDSL predicates (e.g., filter by status,
 * date range, currency).
 */
@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, UUID>, QuerydslPredicateExecutor<PurchaseOrder> {

    /** Find a purchase order by its client-supplied idempotency key. */
    Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);
}
