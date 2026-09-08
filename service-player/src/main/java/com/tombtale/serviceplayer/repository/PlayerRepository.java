package com.tombtale.serviceplayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tombtale.serviceplayer.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for Player entities.
 */
@Repository
public interface PlayerRepository
        extends JpaRepository<Player, Long>, PlayerQueryRepository {

    /**
     * Find a player by Zitadel user ID with their characters already loaded.
     *
     * <p>The {@code /me} endpoint maps characters into its response, and it
     * runs outside a transaction, so the collection has to arrive with the
     * player. One join means one query instead of two.
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the player with its characters initialised, if one exists
     */
    @Query("select p from Player p left join fetch p.characters where p.zitadelUserId = :zitadelUserId")
    Optional<Player> findByZitadelUserIdWithCharacters(String zitadelUserId);

    /** Find a player by their in-game display name. */
    Optional<Player> findByDisplayName(String displayName);

    /** Find a player by public ID. */
    Optional<Player> findByPublicId(UUID publicId);

    /** Check if a display name is already taken (case-insensitive). */
    boolean existsByDisplayNameIgnoreCase(String displayName);
}
