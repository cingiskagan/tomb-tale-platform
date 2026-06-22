package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdatePlayerStatsRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.exception.PlayerNotFoundException;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void listPlayers_success() {
        // Arrange
        PlayerFilterRequest filter = new PlayerFilterRequest("test", null, null);
        Pageable pageable = PageRequest.of(0, 10);

        Player player = new Player();
        player.setId(1L);
        player.setDisplayName("test");
        player.setLevel(1);
        player.setExperiencePoints(0L);

        PlayerResponse response = new PlayerResponse(1L, "test", 1, 0L, Instant.now());
        Page<Player> playerPage = new PageImpl<>(List.of(player));

        when(playerRepository.findByFilter(filter, pageable)).thenReturn(playerPage);
        when(playerMapper.toResponse(player)).thenReturn(response);

        // Act
        Page<PlayerResponse> result = playerService.listPlayers(filter, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(response, result.getContent().get(0));

        verify(playerRepository).findByFilter(filter, pageable);
        verify(playerMapper).toResponse(player);
    }

    @Test
    void updatePlayerStats_success() {
        // Arrange
        Long playerId = 1L;
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(5, 100L);

        Player player = new Player();
        player.setId(playerId);
        player.setLevel(1);
        player.setExperiencePoints(0L);

        PlayerResponse expectedResponse = new PlayerResponse(playerId, "user1", 5, 100L, Instant.now());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerMapper.toResponse(any(Player.class))).thenReturn(expectedResponse);

        // Act
        PlayerResponse result = playerService.updatePlayerStats(playerId, request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals(5, player.getLevel());
        assertEquals(100L, player.getExperiencePoints());

        verify(playerRepository).findById(playerId);
        verify(playerRepository).save(player);
        verify(playerMapper).toResponse(player);
    }

    @Test
    void updatePlayerStats_notFound_throwsException() {
        // Arrange
        Long playerId = 999L;
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(5, 100L);

        when(playerRepository.findById(playerId)).thenReturn(Optional.empty());

        // Act & Assert
        PlayerNotFoundException exception = assertThrows(PlayerNotFoundException.class,
                () -> playerService.updatePlayerStats(playerId, request));

        assertEquals("Player not found with ID: " + playerId, exception.getMessage());

        verify(playerRepository).findById(playerId);
        verify(playerRepository, never()).save(any(Player.class));
        verify(playerMapper, never()).toResponse(any(Player.class));
    }
}
