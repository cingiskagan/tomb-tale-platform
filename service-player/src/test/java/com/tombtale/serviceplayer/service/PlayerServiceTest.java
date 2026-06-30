package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
class PlayerServiceTest {

    private static final int PAGE_SIZE = 10;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void shouldListPlayersSuccessfully() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        Player player = new Player();
        player.setId(1L);
        player.setDisplayName("test");

        PlayerResponse response = new PlayerResponse(
                UUID.randomUUID(), "test", "pi-user", new ArrayList<>(), Instant.now());
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
    void shouldReturnExistingPlayerAndBackfillCharacter() {
        Player existing = new Player();
        existing.setPublicId(UUID.randomUUID());
        existing.setDisplayName("OldName");
        existing.setCharacters(new ArrayList<>());

        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(existing));
        when(playerRepository.save(existing)).thenReturn(existing);

        Player result = playerService.getOrCreatePlayer("z1");

        assertThat(result.getCharacters()).hasSize(1);
        assertThat(result.getCharacters().get(0).getName()).isEqualTo("OldName");
        verify(playerRepository).save(existing);
    }

    @Test
    void shouldReturnExistingPlayerWithoutBackfill() {
        Player existing = new Player();
        GameCharacter character = new GameCharacter();
        existing.getCharacters().add(character);

        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(existing));

        Player result = playerService.getOrCreatePlayer("z1");

        assertThat(result).isEqualTo(existing);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewPlayerIfNotFound() {
        when(playerRepository.findByZitadelUserId("new-z1")).thenReturn(Optional.empty());
        
        Player newPlayer = new Player();
        newPlayer.setDisplayName("Player_new-z1");
        
        when(playerRepository.save(any(Player.class))).thenReturn(newPlayer);

        Player result = playerService.getOrCreatePlayer("new-z1");

        assertThat(result).isEqualTo(newPlayer);
        verify(playerRepository).save(argThat(p -> 
                "Player_new-z1".equals(p.getDisplayName()) && 
                "new-z1".equals(p.getZitadelUserId()) &&
                p.getCharacters().size() == 1
        ));
    }

    @Test
    void shouldUpdateMyProfileSuccess() {
        Player existing = new Player();
        existing.setDisplayName("OldName");

        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName("NewName");
        request.setProfileIcon("new-icon");

        PlayerResponse response = new PlayerResponse(
                UUID.randomUUID(), "NewName", "new-icon", new ArrayList<>(), Instant.now());

        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayName("NewName")).thenReturn(false);
        when(playerRepository.save(existing)).thenReturn(existing);
        when(playerMapper.toResponse(existing)).thenReturn(response);

        PlayerResponse result = playerService.updateMyProfile("z1", request);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getDisplayName()).isEqualTo("NewName");
        assertThat(existing.getProfileIcon()).isEqualTo("new-icon");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingProfile() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.updateMyProfile("z1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void shouldThrowConflictWhenDisplayNameTaken() {
        Player existing = new Player();
        existing.setDisplayName("OldName");

        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName("TakenName");

        when(playerRepository.findByZitadelUserId("z1")).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayName("TakenName")).thenReturn(true);

        assertThatThrownBy(() -> playerService.updateMyProfile("z1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Display name is already taken");
    }
}
