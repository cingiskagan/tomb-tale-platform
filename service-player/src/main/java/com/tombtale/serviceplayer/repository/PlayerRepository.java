package com.tombtale.serviceplayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Find a player by their Zitadel user ID (JWT "sub" claim). */
    Optional<Player> findByZitadelUserId(String zitadelUserId);

    /** Find a player by their in-game display name. */
    Optional<Player> findByDisplayName(String displayName);

    /** Find a player by public ID. */
    Optional<Player> findByPublicId(UUID publicId);

    /** Check if a display name is already taken (case-insensitive). */
    boolean existsByDisplayNameIgnoreCase(String displayName);
}
