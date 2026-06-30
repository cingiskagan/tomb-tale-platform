package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.UpdateCharacterStatsRequest;
import com.tombtale.serviceplayer.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for character operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/players/{publicId}/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    /**
     * PATCH /api/v1/players/{publicId}/characters/{characterPublicId}/stats
     * <p>
     * Updates the core progression stats of a character.
     * Restricted to admin/game master roles.
     *
     * @param publicId           player public ID (used for routing context)
     * @param characterPublicId  character public ID
     * @param request            the new stats
     * @return the updated character
     */
    @PreAuthorize("hasAuthority('platform_admin') or hasAuthority('game_master')")
    @PatchMapping("/{characterPublicId}/stats")
    public ResponseEntity<CharacterResponse> updateCharacterStats(
            @PathVariable UUID publicId,
            @PathVariable UUID characterPublicId,
            @Valid @RequestBody UpdateCharacterStatsRequest request) {
        log.info("Admin updating stats for character: {}, new level: {}, new XP: {}",
                characterPublicId, request.getLevel(), request.getExperiencePoints());
        CharacterResponse updated = characterService.updateCharacterStats(characterPublicId, request);
        return ResponseEntity.ok(updated);
    }
}
