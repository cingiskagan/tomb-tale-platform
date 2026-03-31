package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom query interface for dynamic, type-safe purchase filtering via QueryDSL.
 *
 * <p>Implementations use {@code JPAQueryFactory} to build predicates
 * from {@link PurchaseFilterRequest} fields at runtime.
 */
public interface PurchaseQueryRepository {

    /**
     * Returns a paginated list of purchases matching the given filter criteria.
     *
     * <p>Soft-deleted purchases ({@code CANCELLED}) are excluded by default.
     *
     * @param filter   optional filter fields (all nullable — null = no filter)
     * @param pageable pagination and sorting parameters
     * @return a page of matching purchases
     */
    Page<Purchase> findByFilter(PurchaseFilterRequest filter, Pageable pageable);
}
