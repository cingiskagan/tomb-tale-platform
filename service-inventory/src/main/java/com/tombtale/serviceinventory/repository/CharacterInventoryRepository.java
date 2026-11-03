package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.CharacterInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Reads and writes the characters inventory knows.
 */
public interface CharacterInventoryRepository extends JpaRepository<CharacterInventory, Long> {

    /**
     * Whether inventory already has the character's row.
     *
     * @param publicId the character's {@code publicId}
     * @return true once the character's {@code player.created} was applied
     */
    boolean existsByPublicId(UUID publicId);
}
