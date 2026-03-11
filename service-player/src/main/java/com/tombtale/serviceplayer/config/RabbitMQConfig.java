package com.tombtale.serviceplayer.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Player Service.
 * <p>
 * Declares the player events topic exchange.
 * Other services (economy, achievement, leaderboard) will bind their queues
 * to this exchange with routing keys like:
 * - player.created
 * - player.level-up
 * - player.item-acquired
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";

    /**
     * Topic exchange for player-related events.
     * Topic exchanges route messages to queues based on routing key patterns,
     * e.g., "player.#" matches all player events.
     */
    @Bean
    public TopicExchange playerEventsExchange() {
        // durable=true: survives broker restart
        // autoDelete=false: not deleted when no queues are bound
        return new TopicExchange(PLAYER_EVENTS_EXCHANGE, true, false);
    }

    /**
     * Use Jackson for JSON serialization of messages.
     * This allows us to send POJOs directly and have them auto-serialized.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
