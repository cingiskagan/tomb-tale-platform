package com.tombtale.serviceplayer.service;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.serviceplayer.config.JpaConfig;
import com.tombtale.serviceplayer.config.QueryDslConfig;
import com.tombtale.serviceplayer.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceplayer.entity.OutboxEvent;
import com.tombtale.serviceplayer.repository.OutboxEventRepository;
import com.tombtale.serviceplayer.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations.OperationsCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The real auditor and no test transaction, because Hibernate asks for the author when the publisher's own
 * transaction commits. If that commit falls outside {@code SystemActor.run}, the publish throws.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({ QueryDslConfig.class, JpaConfig.class, OutboxPublisher.class })
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxPublisherAuthorTest extends PostgresTestBase {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

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
    void thePlayerWritesTheRowAndThePublisherMarksIt() {
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

        OutboxEvent published = outboxEventRepository.findById(row.getId()).orElseThrow();
        assertThat(published.getCreatedBy()).isEqualTo(player);
        assertThat(published.getUpdatedBy()).isEqualTo(SystemActor.PLAYER_OUTBOX_PUBLISHER.actorId());
    }
}
