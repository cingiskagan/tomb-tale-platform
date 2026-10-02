package com.tombtale.servicecommerce.config;

import com.tombtale.servicecommerce.dto.event.PlayerCreatedPayload;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds commerce's queue to service-player's exchange.
 * The exchange is declared here too, so commerce can start before service-player.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";
    public static final String PLAYER_CREATED_QUEUE = "commerce.player-created";

    /** Declared with the same arguments as service-player's, or the broker refuses one of them. */
    @Bean
    public TopicExchange playerEventsExchange() {
        return new TopicExchange(PLAYER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue playerCreatedQueue() {
        return QueueBuilder.durable(PLAYER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding playerCreatedBinding(Queue playerCreatedQueue, TopicExchange playerEventsExchange) {
        return BindingBuilder.bind(playerCreatedQueue).to(playerEventsExchange).with(PlayerCreatedPayload.EVENT_TYPE);
    }

    /** Reads the JSON body into the listener's parameter type, generics included. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
