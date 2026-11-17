package com.tombtale.commons.outbox;

import com.tombtale.commons.entity.OutboxEvent;
import com.tombtale.commons.security.PublicIdClaim;
import com.tombtale.commons.support.PostgresTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * transaction commits. If that commit falls outside the owner actor's {@code run}, the publish throws.
 */
@DataJpaTest(showSql = false)
@Import(OutboxPublisher.class)
@ImportAutoConfiguration({ JacksonAutoConfiguration.class, ObservationAutoConfiguration.class })
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
    void theCallerWritesTheRowAndTheOwnersActorMarksIt() {
        UUID player = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "none").claim(PublicIdClaim.CLAIM, player.toString()).build(), List.of()));
        OutboxEvent row = outboxEventRepository.save(OutboxEvent.builder()
                .eventType("test.happened")
                .eventVersion(1)
                .aggregateId(player)
                .payload("{}")
                .build());
        SecurityContextHolder.clearContext();

        assertThat(outboxPublisher.publishPending()).isEqualTo(1);

        OutboxEvent published = outboxEventRepository.findById(row.getId()).orElseThrow();
        assertThat(published.getCreatedBy()).isEqualTo(player);
        assertThat(published.getUpdatedBy()).isEqualTo(OutboxTestApplication.ACTOR.actorId());
    }
}
