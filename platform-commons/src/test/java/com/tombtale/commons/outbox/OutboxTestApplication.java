package com.tombtale.commons.outbox;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.security.PlatformAuditorAware;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The application the outbox tests start, wired as a service wires its outbox. Each test imports
 * the outbox beans it needs, as a service's {@code OutboxConfig} does.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
@Import(OutboxPersistence.class)
class OutboxTestApplication {

    static final String PRODUCER = "service-test";
    static final String EXCHANGE = "test.events";
    static final SystemActor ACTOR = SystemActor.PLAYER_OUTBOX_PUBLISHER;

    @Bean
    AuditorAware<UUID> auditorAware() {
        return new PlatformAuditorAware();
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }

    @Bean
    OutboxOwner outboxOwner() {
        return new OutboxOwner(PRODUCER, EXCHANGE, ACTOR);
    }

    /** Declared on the broker only by the tests that start one. */
    @Bean
    TopicExchange testEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }
}
