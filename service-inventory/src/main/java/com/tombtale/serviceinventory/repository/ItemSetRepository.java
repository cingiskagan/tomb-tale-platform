package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.ItemSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the item sets.
 */
public interface ItemSetRepository extends JpaRepository<ItemSet, Long> {

    /**
     * @param code a set's code
     * @return whether a set has it
     */
    boolean existsByCode(String code);

    /**
     * @param publicId the set's id in the API
     * @return the set, or empty when no set has the id
     */
    Optional<ItemSet> findByPublicId(UUID publicId);
}
