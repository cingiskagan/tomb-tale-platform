package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.CharacterResponse;
import com.tombtale.serviceplayer.dto.UpdateCharacterStatsRequest;
import com.tombtale.serviceplayer.service.CharacterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterControllerTest {

    private static final int HTTP_OK = 200;
    private static final int TEST_LEVEL = 2;
    private static final long TEST_XP = 100L;

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private CharacterController characterController;

    @Test
    void shouldUpdateCharacterStats() {
        UUID publicId = UUID.randomUUID();
        UUID characterPublicId = UUID.randomUUID();
        UpdateCharacterStatsRequest request = new UpdateCharacterStatsRequest(TEST_LEVEL, TEST_XP);

        CharacterResponse responseDto = new CharacterResponse(
                characterPublicId, "TestChar", TEST_LEVEL, TEST_XP, Instant.now());

        when(characterService.updateCharacterStats(publicId, characterPublicId, request)).thenReturn(responseDto);

        ResponseEntity<CharacterResponse> response = characterController.updateCharacterStats(
                publicId, characterPublicId, request);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(characterService).updateCharacterStats(publicId, characterPublicId, request);
    }
}
