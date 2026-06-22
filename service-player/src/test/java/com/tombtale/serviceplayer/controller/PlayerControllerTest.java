package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdatePlayerStatsRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
class PlayerControllerTest {

    private static final int HTTP_OK = 200;
    private static final int PAGE_SIZE = 10;
    private static final long INITIAL_XP = 100L;
    private static final int NEW_LEVEL = 5;
    private static final long NEW_XP = 500L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PlayerController playerController;

    @Test
    void shouldReturnExistingProfile() {
        Player player = new Player();
        player.setZitadelUserId("z1");
        
        when(jwt.getSubject()).thenReturn("z1");
        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(player));

        ResponseEntity<Player> response = playerController.getMyProfile(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().getZitadelUserId()).isEqualTo("z1");
        verify(playerRepository, never()).save(any());
    }

    @Test
    void shouldCreateAndReturnNewProfile() {
        Player newPlayer = new Player();
        newPlayer.setZitadelUserId("new-z1");
        newPlayer.setDisplayName("Player_new-z1");

        when(jwt.getSubject()).thenReturn("new-z1");
        when(playerRepository.findByZitadelUserId("new-z1")).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(newPlayer);

        ResponseEntity<Player> response = playerController.getMyProfile(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().getZitadelUserId()).isEqualTo("new-z1");
        assertThat(response.getBody().getDisplayName()).isEqualTo("Player_new-z1");

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(playerCaptor.capture());

        Player savedPlayer = playerCaptor.getValue();
        assertThat(savedPlayer.getLevel()).isEqualTo(1);
        assertThat(savedPlayer.getExperiencePoints()).isZero();
    }

    @Test
    void shouldReturnPlayerPage() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test", null, null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        PlayerResponse dto = new PlayerResponse(1L, "test", PAGE_SIZE, INITIAL_XP, Instant.now());
        Page<PlayerResponse> page = new PageImpl<>(List.of(dto));

        when(playerService.listPlayers(filter, pageable)).thenReturn(page);

        ResponseEntity<Page<PlayerResponse>> response = playerController.listPlayers(filter, pageable);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().getTotalElements()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateStatsAndReturnProfile() {
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(NEW_LEVEL, NEW_XP);
        PlayerResponse dto = new PlayerResponse(1L, "test", NEW_LEVEL, NEW_XP, Instant.now());

        when(playerService.updatePlayerStats(1L, request)).thenReturn(dto);

        ResponseEntity<PlayerResponse> response = playerController.updatePlayerStats(1L, request);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().level()).isEqualTo(NEW_LEVEL);
    }
}
