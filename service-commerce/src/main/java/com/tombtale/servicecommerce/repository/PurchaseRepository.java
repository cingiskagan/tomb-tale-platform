package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Purchase} entities.
 *
 * <p>Extends {@link PurchaseQueryRepository} so Spring Data automatically
 * wires the QueryDSL-based {@code PurchaseQueryRepositoryImpl} as the
 * custom fragment implementation.
 *
 * <p>The key type is {@code Long} — the internal one. Everything arriving
 * from the API names a purchase by its {@code publicId}, so callers use
 * {@link #findByPublicId(UUID)} and never {@code findById}.
 */
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, PurchaseQueryRepository {

    /**
     * Finds a purchase by the identifier its API exposes.
     *
     * @param publicId the purchase's public UUID
     * @return the purchase, if one carries that id
     */
    Optional<Purchase> findByPublicId(UUID publicId);
}
