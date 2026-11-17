package com.tombtale.serviceplayer.config;

import com.tombtale.commons.audit.SystemActor;
import com.tombtale.commons.outbox.OutboxOwner;
import com.tombtale.commons.outbox.OutboxPublisher;
import com.tombtale.commons.outbox.OutboxService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The outbox from platform-commons. Player's events go to {@code player.events},
 * and its publisher writes as {@link SystemActor#PLAYER_OUTBOX_PUBLISHER}.
 */
@Configuration
@Import({ OutboxService.class, OutboxPublisher.class })
public class OutboxConfig {

    @Bean
    OutboxOwner outboxOwner() {
        return new OutboxOwner("service-player", RabbitMQConfig.PLAYER_EVENTS_EXCHANGE, SystemActor.PLAYER_OUTBOX_PUBLISHER);
    }
}
