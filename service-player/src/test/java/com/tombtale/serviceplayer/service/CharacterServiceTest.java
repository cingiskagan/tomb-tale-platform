package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.UpdateCharacterStatsRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    private static final int TEST_LEVEL = 2;
    private static final long TEST_XP = 100L;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private CharacterService characterService;

    @Test
    void shouldUpdateCharacterStatsSuccessfully() {
        UUID playerPublicId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        GameCharacter existing = new GameCharacter();
        existing.setPublicId(publicId);
        existing.setLevel(1);
        existing.setExperiencePoints(0L);

        UpdateCharacterStatsRequest request = new UpdateCharacterStatsRequest(TEST_LEVEL, TEST_XP);
        CharacterResponse response = new CharacterResponse(
                publicId, "TestChar", TEST_LEVEL, TEST_XP, Instant.now());

        when(characterRepository.findByPublicIdAndPlayerPublicId(publicId, playerPublicId)).thenReturn(Optional.of(existing));
        when(characterRepository.save(existing)).thenReturn(existing);
        when(playerMapper.toResponse(existing)).thenReturn(response);

        CharacterResponse result = characterService.updateCharacterStats(playerPublicId, publicId, request);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getLevel()).isEqualTo(TEST_LEVEL);
        assertThat(existing.getExperiencePoints()).isEqualTo(TEST_XP);
        verify(characterRepository).save(existing);
    }

    @Test
    void shouldThrowWhenCharacterNotFound() {
        UUID playerPublicId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        UpdateCharacterStatsRequest request = new UpdateCharacterStatsRequest(TEST_LEVEL, TEST_XP);

        when(characterRepository.findByPublicIdAndPlayerPublicId(publicId, playerPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.updateCharacterStats(playerPublicId, publicId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Character not found");
    }
}
