package com.tombtale.serviceplayer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitOperations.OperationsCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.persistence.EntityManager;

import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.config.RabbitMQConfig;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.support.PostgresTestBase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The publisher against a real Postgres and a mocked broker. The real broker, down
 * and back up, is the failure test E1 leaves to be written by hand.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class, OutboxPublisher.class })
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class OutboxPublisherTest extends PostgresTestBase {

    private static final String EXCHANGE = RabbitMQConfig.PLAYER_EVENTS_EXCHANGE;
    private static final String EVENT_TYPE = PlayerCreatedPayload.EVENT_TYPE;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.outbox.retention}")
    private Duration retention;

    /**
     * Other classes commit rows into the shared container. Deleting them inside
     * this test's transaction hides them from the publisher and rolls back after.
     */
    @BeforeEach
    void emptyOutboxAndAWorkingBroker() {
        outboxEventRepository.deleteAllInBatch();
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<OperationsCallback<?>>getArgument(0).doInRabbit(rabbitTemplate));
    }

    private OutboxEvent aRow(Instant publishedAt) {
        UUID playerPublicId = UUID.randomUUID();
        return outboxEventRepository.save(OutboxEvent.builder()
                .eventType(EVENT_TYPE)
                .eventVersion(PlayerCreatedPayload.EVENT_VERSION)
                .aggregateId(playerPublicId)
                .payload(objectMapper.writeValueAsString(
                        new PlayerCreatedPayload(playerPublicId, "Player_abc12345", UUID.randomUUID())))
                .publishedAt(publishedAt)
                .build());
    }

    private OutboxEvent reloaded(OutboxEvent row) {
        return outboxEventRepository.findById(row.getId()).orElseThrow();
    }

    private void flushAndForget() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldSendPendingRowsInOrderAndMarkThemSent() {
        OutboxEvent first = aRow(null);
        OutboxEvent second = aRow(null);
        // Postgres keeps microseconds, so the value compared after the reload is truncated to match.
        OutboxEvent alreadySent = aRow(Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MICROS));
        flushAndForget();

        assertThat(outboxPublisher.publishPending()).isEqualTo(2);
        flushAndForget();

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(2)).send(eq(EXCHANGE), eq(EVENT_TYPE), sent.capture());
        verify(rabbitTemplate, times(2)).waitForConfirmsOrDie(anyLong());
        assertThat(sent.getAllValues())
                .extracting(message -> message.getMessageProperties().getMessageId())
                .containsExactly(first.getPublicId().toString(), second.getPublicId().toString());

        assertThat(reloaded(first).getPublishedAt()).isNotNull();
        assertThat(reloaded(second).getPublishedAt()).isNotNull();
        assertThat(reloaded(alreadySent).getPublishedAt()).isEqualTo(alreadySent.getPublishedAt());
    }

    /** The envelope comes from the row's columns, with the stored payload as its data. */
    @Test
    void shouldWrapThePayloadInTheEnvelope() {
        OutboxEvent row = aRow(null);
        flushAndForget();

        outboxPublisher.publishPending();

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE), eq(EVENT_TYPE), sent.capture());
        assertThat(sent.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");

        JsonNode envelope = objectMapper.readTree(sent.getValue().getBody());
        assertThat(envelope.get("eventId").asString()).isEqualTo(row.getPublicId().toString());
        assertThat(envelope.get("eventType").asString()).isEqualTo(EVENT_TYPE);
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(PlayerCreatedPayload.EVENT_VERSION);
        assertThat(Instant.parse(envelope.get("occurredAt").asString())).isEqualTo(reloaded(row).getCreatedAt());
        assertThat(envelope.get("producer").asString()).isEqualTo("service-player");
        assertThat(envelope.get("data").get("playerPublicId").asString())
                .isEqualTo(row.getAggregateId().toString());
    }

    /**
     * The broker drops mid-batch. What it confirmed commits as sent; the row it
     * refused, and the one behind it, wait for the next run.
     */
    @Test
    void shouldLeaveTheRestPendingWhenTheBrokerFails() {
        OutboxEvent confirmed = aRow(null);
        OutboxEvent refused = aRow(null);
        OutboxEvent neverTried = aRow(null);
        flushAndForget();

        doNothing()
                .doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        assertThat(outboxPublisher.publishPending()).isEqualTo(1);
        flushAndForget();

        verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any(Message.class));
        assertThat(reloaded(confirmed).getPublishedAt()).isNotNull();
        assertThat(reloaded(refused).getPublishedAt()).isNull();
        assertThat(reloaded(neverTried).getPublishedAt()).isNull();
    }

    @Test
    void shouldPurgeOnlyRowsPublishedBeforeTheRetention() {
        Instant now = Instant.now();
        aRow(now.minus(retention).minus(Duration.ofDays(1)));
        OutboxEvent recent = aRow(now.minus(Duration.ofDays(1)));
        OutboxEvent pending = aRow(null);
        flushAndForget();

        assertThat(outboxPublisher.purgePublished()).isEqualTo(1);

        List<OutboxEvent> left = outboxEventRepository.findAll();
        assertThat(left).extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(recent.getId(), pending.getId());
    }
}
