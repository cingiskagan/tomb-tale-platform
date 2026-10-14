package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.ItemGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes inventory's item grants, one per purchase.
 */
public interface ItemGrantRepository extends JpaRepository<ItemGrant, Long> {

    /**
     * Finds the item grant inventory already saved for this purchase.
     *
     * @param purchaseId the purchase's {@code publicId}
     * @return the saved item grant, or empty for a purchase inventory has not seen
     */
    Optional<ItemGrant> findByPurchaseId(UUID purchaseId);
}
