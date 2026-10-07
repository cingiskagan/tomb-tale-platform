package com.tombtale.serviceplayer.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.repository.CharacterRepository;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.support.PostgresTestBase;

/**
 * Two first calls for one new player at the same moment, as a double click or two
 * open tabs send them. One insert loses on the unique {@code public_id} and returns the winner's row.
 */
@SpringBootTest
@ActiveProfiles("test")
// The race needs two threads. Each call opens its own transaction, as two requests would.
@SuppressWarnings("PMD.DoNotUseThreads")
class PlayerFirstCallRaceTest extends PostgresTestBase {

    private static final int CALLERS = 2;
    private static final long CALL_TIMEOUT_SECONDS = 30;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private UUID publicId;

    @BeforeEach
    void newPlayer() {
        publicId = UUID.randomUUID();
    }

    /** The calls commit, so the rows outlive the test and would skew other classes' counts. */
    @AfterEach
    void removeCommittedRows() {
        playerRepository.findByPublicIdWithCharacters(publicId).ifPresent(player -> {
            outboxEventRepository.deleteAll(outboxEventRepository.findAll().stream()
                    .filter(event -> event.getAggregateId().equals(publicId))
                    .toList());
            playerRepository.delete(player);
        });
    }

    /** Repeated, because one run can miss the overlap. A run that misses it still passes. */
    @RepeatedTest(5)
    void twoFirstCallsLeaveOnePlayerOneCharacterAndOneEvent() throws Exception {
        String iamId = "iam-" + publicId;
        CyclicBarrier start = new CyclicBarrier(CALLERS);
        List<PlayerResponse> responses = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(CALLERS)) {
            List<Future<PlayerResponse>> calls = new ArrayList<>();
            for (int i = 0; i < CALLERS; i++) {
                calls.add(pool.submit(() -> {
                    start.await();
                    return playerService.getOrCreatePlayer(publicId, iamId);
                }));
            }
            for (Future<PlayerResponse> call : calls) {
                responses.add(call.get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
        }

        assertThat(responses).extracting(PlayerResponse::publicId).containsOnly(publicId);
        // Each caller makes up its own random name, so equal names prove both got the same row.
        assertThat(responses.get(1).displayName()).isEqualTo(responses.get(0).displayName());

        Player stored = playerRepository.findByPublicIdWithCharacters(publicId).orElseThrow();
        assertThat(characterRepository.findByPlayerId(stored.getId())).hasSize(1);
        assertThat(outboxEventRepository.findAll())
                .filteredOn(event -> event.getAggregateId().equals(publicId))
                .hasSize(1);
    }
}
