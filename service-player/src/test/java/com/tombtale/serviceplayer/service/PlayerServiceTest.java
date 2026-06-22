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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class PlayerServiceTest {

    private static final int PAGE_SIZE = 10;
    private static final int NEW_LEVEL = 5;
    private static final long NEW_XP = 100L;
    private static final long NON_EXISTENT_ID = 999L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void shouldListPlayersSuccessfully() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test", null, null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        Player player = new Player();
        player.setId(1L);
        player.setDisplayName("test");
        player.setLevel(1);
        player.setExperiencePoints(0L);

        PlayerResponse response = new PlayerResponse(1L, "test", 1, 0L, Instant.now());
        Page<Player> playerPage = new PageImpl<>(List.of(player));

        when(playerRepository.findByFilter(filter, pageable)).thenReturn(playerPage);
        when(playerMapper.toResponse(player)).thenReturn(response);

        Page<PlayerResponse> result = playerService.listPlayers(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isEqualTo(response);

        verify(playerRepository).findByFilter(filter, pageable);
        verify(playerMapper).toResponse(player);
    }

    @Test
    void shouldUpdatePlayerStatsSuccessfully() {
        Long playerId = 1L;
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(NEW_LEVEL, NEW_XP);

        Player player = new Player();
        player.setId(playerId);
        player.setLevel(1);
        player.setExperiencePoints(0L);

        PlayerResponse expectedResponse = new PlayerResponse(playerId, "user1", NEW_LEVEL, NEW_XP, Instant.now());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerMapper.toResponse(any(Player.class))).thenReturn(expectedResponse);

        PlayerResponse result = playerService.updatePlayerStats(playerId, request);

        assertThat(result).isNotNull().isEqualTo(expectedResponse);
        assertThat(player.getLevel()).isEqualTo(NEW_LEVEL);
        assertThat(player.getExperiencePoints()).isEqualTo(NEW_XP);

        verify(playerRepository).findById(playerId);
        verify(playerRepository).save(player);
        verify(playerMapper).toResponse(player);
    }

    @Test
    void shouldThrowExceptionWhenPlayerNotFound() {
        UpdatePlayerStatsRequest request = new UpdatePlayerStatsRequest(NEW_LEVEL, NEW_XP);

        when(playerRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.updatePlayerStats(NON_EXISTENT_ID, request))
                .isInstanceOf(PlayerNotFoundException.class)
                .hasMessage("Player not found with ID: " + NON_EXISTENT_ID);

        verify(playerRepository).findById(NON_EXISTENT_ID);
        verify(playerRepository, never()).save(any(Player.class));
        verify(playerMapper, never()).toResponse(any(Player.class));
    }
}
