package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB repository for Player documents.
 */
@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {

    /** Find a player by their Zitadel user ID (JWT "sub" claim). */
    Optional<Player> findByZitadelUserId(String zitadelUserId);

    /** Find a player by their in-game display name. */
    Optional<Player> findByDisplayName(String displayName);

    /** Check if a display name is already taken. */
    boolean existsByDisplayName(String displayName);
}
