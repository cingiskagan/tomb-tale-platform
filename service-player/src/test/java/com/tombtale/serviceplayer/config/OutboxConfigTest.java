package com.tombtale.serviceplayer.config;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.entity.OutboxEvent;
import com.tombtale.commons.outbox.OutboxEventRepository;
import com.tombtale.commons.outbox.OutboxPublisher;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitOperations.OperationsCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Player's outbox sends to {@code player.events} as {@code service-player} and marks rows as its own actor.
 * The real auditor and no test transaction, because Hibernate asks for the author when the publisher commits.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class, OutboxConfig.class })
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, ObservationAutoConfiguration.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@SuppressWarnings("PMD.TooManyStaticImports")
class OutboxConfigTest extends PostgresTestBase {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void anEmptyOutboxAndAWorkingBroker() {
        outboxEventRepository.deleteAllInBatch();
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<OperationsCallback<?>>getArgument(0).doInRabbit(rabbitTemplate));
    }

    @AfterEach
    void removeCommittedRows() {
        SecurityContextHolder.clearContext();
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void aPlayerEventGoesToPlayerEventsAndThePlayerPublisherMarksIt() {
        UUID player = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "none").claim(PublicIdClaim.CLAIM, player.toString()).build(), List.of()));
        OutboxEvent row = outboxEventRepository.save(OutboxEvent.builder()
                .eventType(PlayerCreatedPayload.EVENT_TYPE)
                .eventVersion(PlayerCreatedPayload.EVENT_VERSION)
                .aggregateId(player)
                .payload("{}")
                .build());
        SecurityContextHolder.clearContext();

        assertThat(outboxPublisher.publishPending()).isEqualTo(1);

        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq(RabbitMQConfig.PLAYER_EVENTS_EXCHANGE), eq(PlayerCreatedPayload.EVENT_TYPE), sent.capture());
        assertThat(objectMapper.readTree(sent.getValue().getBody()).get("producer").asString())
                .isEqualTo("service-player");
        assertThat(outboxEventRepository.findById(row.getId()).orElseThrow().getUpdatedBy())
                .isEqualTo(SystemActor.PLAYER_OUTBOX_PUBLISHER.actorId());
    }
}
