package com.tombtale.servicecommerce.config;

import com.tombtale.servicecommerce.dto.event.PlayerCreatedPayload;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.listener.FatalExceptionStrategy;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds commerce's queue to service-player's exchange, and gives the queue a DLQ.
 * The exchange is declared here too, so commerce can start before service-player.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";
    public static final String PLAYER_CREATED_QUEUE = "commerce.player-created";
    public static final String PLAYER_CREATED_DLQ = PLAYER_CREATED_QUEUE + ".dlq";

    /** Declared with the same arguments as service-player's, or the broker refuses one of them. */
    @Bean
    public TopicExchange playerEventsExchange() {
        return new TopicExchange(PLAYER_EVENTS_EXCHANGE, true, false);
    }

    /** A rejected message goes through the default exchange, which routes by queue name, to the DLQ. */
    @Bean
    public Queue playerCreatedQueue() {
        return QueueBuilder.durable(PLAYER_CREATED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(PLAYER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue playerCreatedDeadLetterQueue() {
        return QueueBuilder.durable(PLAYER_CREATED_DLQ).build();
    }

    @Bean
    public Binding playerCreatedBinding(Queue playerCreatedQueue, TopicExchange playerEventsExchange) {
        return BindingBuilder.bind(playerCreatedQueue).to(playerEventsExchange).with(PlayerCreatedPayload.EVENT_TYPE);
    }

    /** A message that no retry can fix, such as one that does not convert, goes to the DLQ at once. */
    @Bean
    public RabbitListenerRetrySettingsCustomizer noRetryForFatalErrors() {
        FatalExceptionStrategy fatal = new ConditionalRejectingErrorHandler.DefaultExceptionStrategy();
        return settings -> settings.setExceptionPredicate(exception -> !fatal.isFatal(exception));
    }

    /** Reads the JSON body into the listener's parameter type, generics included. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
