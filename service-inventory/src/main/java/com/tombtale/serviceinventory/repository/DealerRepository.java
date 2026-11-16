package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the dealers.
 */
public interface DealerRepository extends JpaRepository<Dealer, Long> {

    /**
     * @param code a dealer's code
     * @return whether a dealer, retired or not, has it
     */
    boolean existsByCode(String code);

    /**
     * @param code the dealer's code, such as {@code TOWN_BLACKSMITH}
     * @return the dealer, or empty when no dealer has the code
     */
    Optional<Dealer> findByCode(String code);

    /**
     * @param publicId the dealer's id in the API
     * @return the dealer, or empty when no dealer has the id
     */
    Optional<Dealer> findByPublicId(UUID publicId);
}
