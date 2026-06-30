package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for GameCharacter entities.
 */
@Repository
public interface CharacterRepository extends JpaRepository<GameCharacter, Long> {

    List<GameCharacter> findByPlayerId(Long playerId);

    Optional<GameCharacter> findByPublicId(UUID publicId);
}
