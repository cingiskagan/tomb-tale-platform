package com.tombtale.serviceplayer.repository;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.support.PostgresTestBase;

/**
 * Two publishers never send the same row: the pending query skips rows another
 * transaction holds instead of waiting for them. Real commits, so no test transaction.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxPendingLockTest extends PostgresTestBase {

    private static final Limit ALL_PENDING = Limit.of(100);
    private static final long WAIT_SECONDS = 5;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<Long> committed = new ArrayList<>();

    @AfterEach
    void removeCommittedRows() {
        outboxEventRepository.deleteAllByIdInBatch(committed);
    }

    @Test
    void shouldSkipRowsAnotherPublisherHolds() {
        committed.add(outboxEventRepository.save(OutboxEvent.builder()
                .eventType("player.created")
                .eventVersion(1)
                .aggregateId(UUID.randomUUID())
                .payload("{}")
                .build()).getId());

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<OutboxEvent> seen = transaction.execute(first -> {
            outboxEventRepository.findPendingForUpdate(ALL_PENDING);
            // Without SKIP LOCKED the second publisher waits for this lock, so the timeout is the assertion.
            return CompletableFuture.supplyAsync(() -> transaction.execute(second ->
                    outboxEventRepository.findPendingForUpdate(ALL_PENDING)))
                    .orTimeout(WAIT_SECONDS, SECONDS)
                    .join();
        });

        assertThat(seen).extracting(OutboxEvent::getId).doesNotContainAnyElementsOf(committed);
    }
}
