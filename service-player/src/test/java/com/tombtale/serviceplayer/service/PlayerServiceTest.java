package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// TooManyMethods: one test class per service is the convention here, and this
// service has three public methods with several cases each. Splitting by method
// would scatter the shared mocks rather than simplify anything.
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class PlayerServiceTest {

    private static final int PAGE_SIZE = 10;

    /** The {@code public_id} claim of the caller's token. */
    private static final UUID PUBLIC_ID = UUID.fromString("6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f3b64");

    /** The {@code sub} claim: Keycloak's own user id, a different value on purpose. */
    private static final String KEYCLOAK_ID = "a81ce38d-9155-4ee1-8f16-5850387e8029";

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private OutboxService outboxService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PlayerService playerService;

    /**
     * Runs the callback the creation path hands to the template, which a mock
     * otherwise swallows. Only the tests that create a player need it.
     */
    private void runTheTransaction() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> work = invocation.getArgument(0);
            return work.doInTransaction(null);
        });
    }

    /** Content irrelevant: these tests assert on the repository, not on the mapping. */
    private static PlayerResponse aPlayerResponse() {
        return new PlayerResponse(
                UUID.randomUUID(), "test", "pi-user", new ArrayList<>(), Instant.now());
    }

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
    void shouldReturnExistingPlayerWithoutWriting() {
        Player existing = new Player();
        GameCharacter character = new GameCharacter();
        existing.addCharacter(character);
        PlayerResponse response = aPlayerResponse();

        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.of(existing));
        when(playerMapper.toResponse(existing)).thenReturn(response);

        PlayerResponse result = playerService.getOrCreatePlayer(PUBLIC_ID, KEYCLOAK_ID);

        assertThat(result).isEqualTo(response);
        verify(playerRepository, never()).save(any());
    }

    /** The new row takes both ids from the token: the publicId every service shares, and the sub. */
    @Test
    void shouldCreateNewPlayerWithTheTokensPublicId() {
        runTheTransaction();
        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.empty());

        Player newPlayer = new Player();
        newPlayer.setDisplayName("Player_random1");

        when(playerRepository.save(any(Player.class))).thenReturn(newPlayer);
        when(playerMapper.toResponse(newPlayer)).thenReturn(aPlayerResponse());

        PlayerResponse result = playerService.getOrCreatePlayer(PUBLIC_ID, KEYCLOAK_ID);

        assertThat(result).isNotNull();
        verify(playerRepository).save(argThat(p ->
                PUBLIC_ID.equals(p.getPublicId())
                        && KEYCLOAK_ID.equals(p.getKeycloakId())
                        && p.getDisplayName() != null && p.getDisplayName().startsWith("Player_")
                        && p.getCharacters().size() == 1));
    }

    /**
     * The event is written from inside the template's callback, so it shares the
     * transaction the player and the character commit in.
     */
    @Test
    void shouldWriteThePlayerCreatedEventWithTheNewPlayer() {
        runTheTransaction();
        Player saved = new Player();
        saved.setDisplayName("Player_random1");

        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(saved);
        when(playerMapper.toResponse(saved)).thenReturn(aPlayerResponse());

        playerService.getOrCreatePlayer(PUBLIC_ID, KEYCLOAK_ID);

        ArgumentCaptor<PlayerCreatedPayload> payload = ArgumentCaptor.forClass(PlayerCreatedPayload.class);
        verify(outboxService).append(
                eq(PlayerCreatedPayload.EVENT_TYPE),
                eq(PlayerCreatedPayload.EVENT_VERSION),
                eq(saved.getPublicId()),
                payload.capture());

        assertThat(payload.getValue().playerPublicId()).isEqualTo(saved.getPublicId());
        assertThat(payload.getValue().displayName()).isEqualTo("Player_random1");
        assertThat(payload.getValue().characterPublicId()).isNotNull();
    }

    @Test
    void shouldRecoverFromConcurrentCreationConflict() {
        runTheTransaction();
        Player winner = new Player();

        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID))
                .thenReturn(Optional.empty()) // First check: not found
                .thenReturn(Optional.of(winner)); // Second check after exception: found

        when(playerRepository.save(any(Player.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));
        when(playerMapper.toResponse(winner)).thenReturn(aPlayerResponse());

        PlayerResponse result = playerService.getOrCreatePlayer(PUBLIC_ID, KEYCLOAK_ID);

        assertThat(result).isNotNull();
        verify(playerRepository, times(2)).findByPublicIdWithCharacters(PUBLIC_ID);
        // The losing call writes once and never again: the winner's row is returned as it stands.
        verify(playerRepository, times(1)).save(any(Player.class));
    }

    @Test
    void shouldThrowIfRecoverFromConcurrentCreationFails() {
        runTheTransaction();
        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID))
                .thenReturn(Optional.empty()) // First check: not found
                .thenReturn(Optional.empty()); // Second check after exception: still not found!

        when(playerRepository.save(any(Player.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> playerService.getOrCreatePlayer(PUBLIC_ID, KEYCLOAK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to find player after creation collision");
    }

    @Test
    void shouldUpdateMyProfileSuccess() {
        Player existing = new Player();
        existing.setDisplayName("OldName");

        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName("NewName");

        PlayerResponse response = new PlayerResponse(
                UUID.randomUUID(), "NewName", "pi-user", new ArrayList<>(), Instant.now());

        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayNameIgnoreCase("NewName")).thenReturn(false);
        when(playerRepository.save(existing)).thenReturn(existing);
        when(playerMapper.toResponse(existing)).thenReturn(response);

        PlayerResponse result = playerService.updateMyProfile(PUBLIC_ID, request);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getDisplayName()).isEqualTo("NewName");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingProfile() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.updateMyProfile(PUBLIC_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void shouldThrowConflictWhenDisplayNameTaken() {
        Player existing = new Player();
        existing.setDisplayName("OldName");

        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        request.setDisplayName("TakenName");

        when(playerRepository.findByPublicIdWithCharacters(PUBLIC_ID)).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayNameIgnoreCase("TakenName")).thenReturn(true);

        assertThatThrownBy(() -> playerService.updateMyProfile(PUBLIC_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Display name is already taken");
    }
}
