package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.Dealer;
import com.tombtale.serviceinventory.entity.StandEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the entries on dealers' stands.
 */
public interface StandEntryRepository extends JpaRepository<StandEntry, Long> {

    /**
     * The entries a stand shows, oldest first, with their templates. A retired template leaves every stand.
     *
     * @param dealer the dealer
     * @return its entries whose template is not retired
     */
    @Query("select e from StandEntry e join fetch e.template t where e.dealer = :dealer and t.retired = false "
            + "order by e.id")
    List<StandEntry> findOnStand(@Param("dealer") Dealer dealer);

    /**
     * @param publicId the entry's id in the API
     * @param dealer   the dealer whose stand it must be on
     * @return the entry, or empty when that dealer has none with the id
     */
    Optional<StandEntry> findByPublicIdAndDealer(UUID publicId, Dealer dealer);
}
