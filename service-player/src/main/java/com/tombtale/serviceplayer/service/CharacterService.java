package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.UpdateCharacterStatsRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Business-logic layer for character operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final PlayerMapper playerMapper;

    /**
     * Updates the core progression stats (level, XP) of a specific character.
     *
     * @param playerPublicId    the public ID of the player who owns the character
     * @param characterPublicId the public ID of the character
     * @param request           the new stats payload
     * @return the updated character DTO
     * @throws ResponseStatusException 404 if the character is not found or doesn't belong to the player
     */
    @Transactional
    public CharacterResponse updateCharacterStats(UUID playerPublicId, UUID characterPublicId, UpdateCharacterStatsRequest request) {
        GameCharacter character = characterRepository.findByPublicIdAndPlayerPublicId(characterPublicId, playerPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character not found or access denied: " + characterPublicId));
        
        character.setLevel(request.getLevel());
        character.setExperiencePoints(request.getExperiencePoints());
        
        GameCharacter saved = characterRepository.save(character);
        return playerMapper.toResponse(saved);
    }
}
