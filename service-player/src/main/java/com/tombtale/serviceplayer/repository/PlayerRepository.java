package com.tombtale.serviceplayer.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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

    /**
     * Resolve a Zitadel subject to the player's {@code publicId}.
     *
     * <p>Called by {@code PlayerAuditorAware} to fill {@code createdBy} and
     * {@code updatedBy}, which happens inside a persist or a flush. A normal
     * query would ask Hibernate to flush first, in the middle of the flush
     * that triggered it, so this one is pinned to {@code FlushMode.COMMIT}.
     * It only ever reads a row that is already committed.
     *
     * @param zitadelUserId the subject claim from the JWT
     * @return the player's public ID, if that subject has a profile yet
     */
    @Query("select p.publicId from Player p where p.zitadelUserId = :zitadelUserId")
    @QueryHints(@QueryHint(name = "org.hibernate.flushMode", value = "COMMIT"))
    Optional<UUID> findPublicIdByZitadelUserId(String zitadelUserId);

    /** Find a player by their in-game display name. */
    Optional<Player> findByDisplayName(String displayName);

    /** Find a player by public ID. */
    Optional<Player> findByPublicId(UUID publicId);

    /** Check if a display name is already taken (case-insensitive). */
    boolean existsByDisplayNameIgnoreCase(String displayName);
}
