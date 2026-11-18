package com.tombtale.commons.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import com.tombtale.commons.entity.OutboxEvent;
import com.tombtale.commons.support.FixedAuthorTestBase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The outbox write against a real Postgres: the jsonb column, and the rule that
 * an event is only ever written inside the transaction that produced the fact.
 */
@DataJpaTest(showSql = false)
@Import(OutboxService.class)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, ObservationAutoConfiguration.class })
class OutboxServiceTest extends FixedAuthorTestBase {

    private static final String EVENT_TYPE = "test.happened";
    private static final int LEVEL = 7;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private UUID aggregateId;

    /** A payload shaped like a service's own event record. */
    record Happened(UUID characterPublicId, int level) {
    }

    @BeforeEach
    void freshEvent() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        aggregateId = UUID.randomUUID();
    }

    private OutboxEvent appendEvent() {
        return outboxService.append(EVENT_TYPE, 1, aggregateId, new Happened(aggregateId, LEVEL));
    }

    private List<OutboxEvent> committedRows() {
        return outboxEventRepository.findAll().stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .toList();
    }

    /**
     * The row the publisher will read: the envelope in columns, the data object
     * in jsonb, and no publication time yet.
     */
    @Test
    void shouldStoreTheEventAsPendingJson() {
        OutboxEvent written = appendEvent();
        entityManager.flush();
        entityManager.clear();

        OutboxEvent row = outboxEventRepository.findById(written.getId()).orElseThrow();
        assertThat(row.getPublicId()).isEqualTo(written.getPublicId());
        assertThat(row.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(row.getEventVersion()).isEqualTo(1);
        assertThat(row.getAggregateId()).isEqualTo(aggregateId);
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getPublishedAt()).as("a fresh row is pending").isNull();

        JsonNode data = objectMapper.readTree(row.getPayload());
        assertThat(data.propertyNames()).containsExactlyInAnyOrder("characterPublicId", "level");
        assertThat(data.get("characterPublicId").asString()).isEqualTo(aggregateId.toString());
        assertThat(data.get("level").asInt()).isEqualTo(LEVEL);
    }

    /**
     * MANDATORY is the whole point of the pattern. An event written in a
     * transaction of its own is the dual write the outbox exists to prevent.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRefuseAnEventWithNoTransactionToJoin() {
        assertThatThrownBy(this::appendEvent)
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(committedRows()).isEmpty();
    }

    /** The fact fails, so the intent to publish it fails with it. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldLeaveNoRowWhenTheTransactionRollsBack() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            appendEvent();
            throw new IllegalStateException("the write after the event fails");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(committedRows()).isEmpty();
    }
}
