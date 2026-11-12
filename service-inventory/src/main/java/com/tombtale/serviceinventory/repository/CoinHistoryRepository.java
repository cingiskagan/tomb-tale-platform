package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.CoinHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Reads and writes the history of every character's coins.
 */
public interface CoinHistoryRepository extends JpaRepository<CoinHistory, Long> {

    /**
     * @param character the character whose coins changed
     * @param reference what made the change, such as a trade id
     * @return whether the character already has a change with the reference, so a trade sent twice is found
     */
    boolean existsByCharacterInventoryAndReference(CharacterInventory character, UUID reference);
}
