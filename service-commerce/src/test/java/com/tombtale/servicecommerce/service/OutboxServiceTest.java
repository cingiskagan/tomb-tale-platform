package com.tombtale.servicecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import com.tombtale.servicecommerce.config.JpaConfig;
import com.tombtale.servicecommerce.config.QueryDslConfig;
import com.tombtale.servicecommerce.dto.event.PurchaseCreatedPayload;
import com.tombtale.servicecommerce.entity.OutboxEvent;
import com.tombtale.servicecommerce.repository.OutboxEventRepository;
import com.tombtale.servicecommerce.support.FixedAuthorTestBase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The outbox write against a real Postgres, ported from service-player: the jsonb column, and the rule
 * that an event is only ever written inside the transaction that produced the fact.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class, OutboxService.class })
@ImportAutoConfiguration(ObservationAutoConfiguration.class)
class OutboxServiceTest extends FixedAuthorTestBase {

    private static final String ITEM_CODE = "SWORD_IRON";

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

    /** @DataJpaTest brings no Jackson, and the service serializes the payload with it. */
    @TestConfiguration
    static class JacksonBean {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @BeforeEach
    void freshEvent() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        aggregateId = UUID.randomUUID();
    }

    private OutboxEvent appendPurchaseCreated() {
        return outboxService.append(
                PurchaseCreatedPayload.EVENT_TYPE,
                PurchaseCreatedPayload.EVENT_VERSION,
                aggregateId,
                new PurchaseCreatedPayload(aggregateId, UUID.randomUUID(), ITEM_CODE, 2));
    }

    private List<OutboxEvent> committedRows() {
        return outboxEventRepository.findAll().stream()
                .filter(event -> aggregateId.equals(event.getAggregateId()))
                .toList();
    }

    @Test
    void shouldStoreTheEventAsPendingJson() {
        OutboxEvent written = appendPurchaseCreated();
        entityManager.flush();
        entityManager.clear();

        OutboxEvent row = outboxEventRepository.findById(written.getId()).orElseThrow();
        assertThat(row.getPublicId()).isEqualTo(written.getPublicId());
        assertThat(row.getEventType()).isEqualTo("purchase.created");
        assertThat(row.getEventVersion()).isEqualTo(1);
        assertThat(row.getAggregateId()).isEqualTo(aggregateId);
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getPublishedAt()).as("a fresh row is pending").isNull();

        JsonNode data = objectMapper.readTree(row.getPayload());
        assertThat(data.get("purchasePublicId").asString()).isEqualTo(aggregateId.toString());
        assertThat(data.get("itemCode").asString()).isEqualTo(ITEM_CODE);
        assertThat(data.get("quantity").asInt()).isEqualTo(2);
    }

    /** An event written in a transaction of its own is the dual write the outbox exists to prevent. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRefuseAnEventWithNoTransactionToJoin() {
        assertThatThrownBy(this::appendPurchaseCreated)
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(committedRows()).isEmpty();
    }

    /** The fact fails, so the intent to publish it fails with it. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldLeaveNoRowWhenTheTransactionRollsBack() {
        assertThatThrownBy(() -> transactionTemplate.execute(status -> {
            appendPurchaseCreated();
            throw new IllegalStateException("the write after the event fails");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(committedRows()).isEmpty();
    }
}
