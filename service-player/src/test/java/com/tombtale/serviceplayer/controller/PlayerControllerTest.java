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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PlayerController playerController;

    @Test
    void getMyProfile_existingUser_returnsProfile() {
        Player player = new Player();
        player.setZitadelUserId("z1");
        
        when(jwt.getSubject()).thenReturn("z1");
        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(player));

        ResponseEntity<Player> response = playerController.getMyProfile(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("z1", response.getBody().getZitadelUserId());
        verify(playerRepository, never()).save(any());
    }

    @Test
    void getMyProfile_newUser_createsAndReturnsProfile() {
        Player newPlayer = new Player();
        newPlayer.setZitadelUserId("new-z1");
        newPlayer.setDisplayName("Player_new-z1");

        when(jwt.getSubject()).thenReturn("new-z1");
        when(playerRepository.findByZitadelUserId("new-z1")).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(newPlayer);

        ResponseEntity<Player> response = playerController.getMyProfile(jwt);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("new-z1", response.getBody().getZitadelUserId());
        assertEquals("Player_new-z1", response.getBody().getDisplayName());
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void listPlayers_returnsPage() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test", null, null);
        Pageable pageable = PageRequest.of(0, 10);
        PlayerResponse dto = new PlayerResponse(1L, "test", 10, 100L, Instant.now());
        Page<PlayerResponse> page = new PageImpl<>(List.of(dto));

        when(playerService.listPlayers(filter, pageable)).thenReturn(page);

        ResponseEntity<Page<PlayerResponse>> response = playerController.listPlayers(filter, pageable);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void updatePlayerStats_updatesAndReturns() {
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(5, 500L);
        PlayerResponse dto = new PlayerResponse(1L, "test", 5, 500L, Instant.now());

        when(playerService.updatePlayerStats(1L, request)).thenReturn(dto);

        ResponseEntity<PlayerResponse> response = playerController.updatePlayerStats(1L, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5, response.getBody().level());
    }
}
