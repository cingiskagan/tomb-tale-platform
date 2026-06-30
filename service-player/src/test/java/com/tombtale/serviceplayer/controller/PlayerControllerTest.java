package com.tombtale.serviceplayer.controller;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
class PlayerControllerTest {

    private static final int HTTP_OK = 200;
    private static final int PAGE_SIZE = 10;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private PlayerController playerController;

    @Test
    void shouldReturnExistingProfile() {
        Player player = new Player();
        player.setZitadelUserId("z1");
        
        PlayerResponse playerResponse = new PlayerResponse(
                UUID.randomUUID(), "Player_z1", "pi-user", new ArrayList<>(), Instant.now());

        when(jwt.getSubject()).thenReturn("z1");
        when(playerService.getOrCreatePlayer("z1")).thenReturn(player);
        when(playerMapper.toResponse(player)).thenReturn(playerResponse);

        ResponseEntity<PlayerResponse> response = playerController.getMyProfile(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().displayName()).isEqualTo("Player_z1");
        verify(playerService).getOrCreatePlayer("z1");
    }

    @Test
    void shouldCreateAndReturnNewProfile() {
        Player newPlayer = new Player();
        newPlayer.setZitadelUserId("new-z1");
        newPlayer.setDisplayName("Player_new-z1");
        
        PlayerResponse playerResponse = new PlayerResponse(
                UUID.randomUUID(), "Player_new-z1", "pi-user", new ArrayList<>(), Instant.now());

        when(jwt.getSubject()).thenReturn("new-z1");
        when(playerService.getOrCreatePlayer("new-z1")).thenReturn(newPlayer);
        when(playerMapper.toResponse(newPlayer)).thenReturn(playerResponse);

        ResponseEntity<PlayerResponse> response = playerController.getMyProfile(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().displayName()).isEqualTo("Player_new-z1");
        verify(playerService).getOrCreatePlayer("new-z1");
    }

    @Test
    void shouldReturnPlayerPage() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        PlayerResponse dto = new PlayerResponse(
                UUID.randomUUID(), "test", "pi-user", new ArrayList<>(), Instant.now());
        Page<PlayerResponse> page = new PageImpl<>(List.of(dto));

        when(playerService.listPlayers(filter, pageable)).thenReturn(page);

        ResponseEntity<Page<PlayerResponse>> response = playerController.listPlayers(filter, pageable);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().getTotalElements()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateMyProfile() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName("NewName");
        
        PlayerResponse playerResponse = new PlayerResponse(
                UUID.randomUUID(), "NewName", "pi-user", new ArrayList<>(), Instant.now());

        when(jwt.getSubject()).thenReturn("z1");
        when(playerService.updateMyProfile("z1", request)).thenReturn(playerResponse);

        ResponseEntity<PlayerResponse> response = playerController.updateMyProfile(jwt, request);

        assertThat(response.getStatusCode().value()).isEqualTo(HTTP_OK);
        assertThat(response.getBody().displayName()).isEqualTo("NewName");
        verify(playerService).updateMyProfile("z1", request);
    }
}
