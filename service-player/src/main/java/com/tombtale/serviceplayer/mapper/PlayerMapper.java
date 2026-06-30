package com.tombtale.serviceplayer.mapper;

import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;

import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper converting between {@link Player} entities and
 * their DTO representations.
 *
 * <p>
 * The {@code zitadelUserId} and {@code updatedAt} fields are
 * automatically excluded because they don't exist in
 * {@link PlayerResponse}.
 */
@Mapper(componentModel = "spring")
public interface PlayerMapper {

    /**
     * Converts a persisted entity to its API response representation.
     *
     * @param player the JPA entity
     * @return the response DTO
     */
    PlayerResponse toResponse(Player player);

    /**
     * Converts a persisted character entity to its API response representation.
     *
     * @param character the JPA entity
     * @return the response DTO
     */
    CharacterResponse toResponse(GameCharacter character);

    /**
     * Converts a list of entities to a list of response DTOs.
     *
     * @param players the entity list
     * @return the response DTO list
     */
    List<PlayerResponse> toResponseList(List<Player> players);
}
