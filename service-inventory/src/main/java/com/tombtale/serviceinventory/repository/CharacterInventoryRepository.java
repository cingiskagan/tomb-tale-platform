package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.CharacterInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
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

    /**
     * The ids of the player's characters. It loads no row, so the lock taken next is the row's first read.
     *
     * @param playerId the owner's {@code publicId}
     * @return the {@code publicId} of every character inventory knows for the player
     */
    @Query("select c.publicId from CharacterInventory c where c.playerId = :playerId")
    List<UUID> findPublicIdsByPlayerId(@Param("playerId") UUID playerId);

    /**
     * The character's row, locked until the transaction ends, so a second write for the character waits for it.
     * Every write to a character's inventory calls this first.
     *
     * @param publicId the character's {@code publicId}
     * @return the locked row, or empty when inventory does not know the character
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CharacterInventory c where c.publicId = :publicId")
    Optional<CharacterInventory> findByPublicIdForUpdate(@Param("publicId") UUID publicId);
}
