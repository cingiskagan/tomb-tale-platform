package com.tombtale.serviceplayer.service;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.dto.UpdateMyProfileRequest;
import com.tombtale.serviceplayer.entity.GameCharacter;
import com.tombtale.serviceplayer.entity.Player;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tombtale.serviceplayer.mapper.PlayerMapper;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
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
// TooManyMethods: one test class per service is the convention here, and this
// service has four public methods with several cases each. Splitting by method
// would scatter the shared mocks rather than simplify anything.
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class PlayerServiceTest {

    private static final int PAGE_SIZE = 10;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerService playerService;

    private ch.qos.logback.classic.Logger fallbackLogger;
    private ListAppender<ILoggingEvent> attachedAppender;
    private Level originalLevel;

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

        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.of(existing));
        when(playerMapper.toResponse(existing)).thenReturn(response);

        PlayerResponse result = playerService.getOrCreatePlayer("z1");

        assertThat(result).isEqualTo(response);
        verify(playerRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewPlayerIfNotFound() {
        when(playerRepository.findByZitadelUserIdWithCharacters("new-z1")).thenReturn(Optional.empty());
        
        Player newPlayer = new Player();
        newPlayer.setDisplayName("Player_random1");
        
        when(playerRepository.save(any(Player.class))).thenReturn(newPlayer);
        when(playerMapper.toResponse(newPlayer)).thenReturn(aPlayerResponse());

        PlayerResponse result = playerService.getOrCreatePlayer("new-z1");

        assertThat(result).isNotNull();
        verify(playerRepository).save(argThat(p -> 
                p.getDisplayName() != null && p.getDisplayName().startsWith("Player_") && 
                "new-z1".equals(p.getZitadelUserId()) &&
                p.getCharacters().size() == 1
        ));
    }

    /**
     * The alarm is the only thing that says the Zitadel event is not working,
     * so it gets a test. Creating a row here means provisioning never ran.
     */
    @Test
    void shouldRaiseTheAlarmWhenItHasToCreateTheRow() {
        ListAppender<ILoggingEvent> captured = captureFallbackLog();

        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(new Player());
        when(playerMapper.toResponse(any(Player.class))).thenReturn(aPlayerResponse());

        playerService.getOrCreatePlayer("z1");

        assertThat(captured.list)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage()).contains("provisioning event never arrived");
                    // The subject is masked, never logged whole.
                    assertThat(event.getFormattedMessage()).doesNotContain("z1");
                });
    }

    /**
     * The same write through the event path says nothing: there it is the design
     * working, not a misconfiguration.
     */
    @Test
    void shouldProvisionSilentlyForTheEventPath() {
        ListAppender<ILoggingEvent> captured = captureFallbackLog();

        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.empty());
        when(playerRepository.save(any(Player.class))).thenReturn(new Player());

        playerService.provisionPlayer("z1");

        assertThat(captured.list).isEmpty();
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void shouldNotWriteWhenTheEventArrivesTwice() {
        when(playerRepository.findByZitadelUserIdWithCharacters("z1"))
                .thenReturn(Optional.of(new Player()));

        playerService.provisionPlayer("z1");

        verify(playerRepository, never()).save(any());
    }

    /**
     * Attaches a captor to the alarm category, raising its level for the length
     * of one test. {@code logback-test.xml} keeps it OFF everywhere else.
     *
     * <p>Restored in {@link #restoreFallbackLogger()}: Surefire runs every class
     * in one JVM, so a logger left switched on here would follow the suite into
     * the next class. JUnit builds a fresh instance per test, so the field starts
     * null again on its own.
     */
    private ListAppender<ILoggingEvent> captureFallbackLog() {
        fallbackLogger = ((LoggerContext) LoggerFactory.getILoggerFactory())
                .getLogger("com.tombtale.provisioning.fallback");
        originalLevel = fallbackLogger.getLevel();
        fallbackLogger.setLevel(Level.ERROR);

        attachedAppender = new ListAppender<>();
        attachedAppender.start();
        fallbackLogger.addAppender(attachedAppender);
        return attachedAppender;
    }

    @AfterEach
    void restoreFallbackLogger() {
        if (fallbackLogger != null) {
            fallbackLogger.detachAppender(attachedAppender);
            fallbackLogger.setLevel(originalLevel);
        }
    }

    @Test
    void shouldRecoverFromConcurrentCreationConflict() {
        Player winner = new Player();

        when(playerRepository.findByZitadelUserIdWithCharacters("z1"))
                .thenReturn(Optional.empty()) // First check: not found
                .thenReturn(Optional.of(winner)); // Second check after exception: found

        when(playerRepository.save(any(Player.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));
        when(playerMapper.toResponse(winner)).thenReturn(aPlayerResponse());

        PlayerResponse result = playerService.getOrCreatePlayer("z1");

        assertThat(result).isNotNull();
        // It should call find twice
        verify(playerRepository, org.mockito.Mockito.times(2)).findByZitadelUserIdWithCharacters("z1");
        // The losing thread writes once and never again: the winner's row is returned as it stands.
        verify(playerRepository, org.mockito.Mockito.times(1)).save(any(Player.class));
    }

    @Test
    void shouldThrowIfRecoverFromConcurrentCreationFails() {
        when(playerRepository.findByZitadelUserIdWithCharacters("z1"))
                .thenReturn(Optional.empty()) // First check: not found
                .thenReturn(Optional.empty()); // Second check after exception: still not found!

        when(playerRepository.save(any(Player.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        assertThatThrownBy(() -> playerService.getOrCreatePlayer("z1"))
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

        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayNameIgnoreCase("NewName")).thenReturn(false);
        when(playerRepository.save(existing)).thenReturn(existing);
        when(playerMapper.toResponse(existing)).thenReturn(response);

        PlayerResponse result = playerService.updateMyProfile("z1", request);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getDisplayName()).isEqualTo("NewName");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingProfile() {
        UpdateMyProfileRequest request = new UpdateMyProfileRequest();
        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.empty());

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

        when(playerRepository.findByZitadelUserIdWithCharacters("z1")).thenReturn(Optional.of(existing));
        when(playerRepository.existsByDisplayNameIgnoreCase("TakenName")).thenReturn(true);

        assertThatThrownBy(() -> playerService.updateMyProfile("z1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Display name is already taken");
    }
}
