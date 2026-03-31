package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Purchase} entities.
 *
 * <p>Extends {@link PurchaseQueryRepository} so Spring Data automatically
 * wires the QueryDSL-based {@code PurchaseQueryRepositoryImpl} as the
 * custom fragment implementation.
 */
public interface PurchaseRepository extends JpaRepository<Purchase, UUID>, PurchaseQueryRepository {
}
