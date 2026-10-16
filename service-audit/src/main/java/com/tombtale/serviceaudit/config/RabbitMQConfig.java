package com.tombtale.serviceaudit.config;

import com.tombtale.serviceaudit.exception.UnlistedEventException;
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
 * One queue on every producer's exchange, with a DLQ. Each exchange is declared here too, so audit can start first.
 * With this queue bound to every routing key, the {@code *.unrouted} queues stay empty.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";
    public static final String PURCHASE_EVENTS_EXCHANGE = "purchase.events";
    public static final String ITEM_EVENTS_EXCHANGE = "item.events";

    public static final String EVENTS_QUEUE = "audit.events";
    public static final String EVENTS_DLQ = EVENTS_QUEUE + ".dlq";

    private static final String EVERY_EVENT = "#";

    /** Declared with the same arguments as the producer's, or the broker refuses one of them. */
    @Bean
    public TopicExchange playerEventsExchange() {
        return new TopicExchange(PLAYER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange purchaseEventsExchange() {
        return new TopicExchange(PURCHASE_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange itemEventsExchange() {
        return new TopicExchange(ITEM_EVENTS_EXCHANGE, true, false);
    }

    /** A rejected message goes through the default exchange, which routes by queue name, to the DLQ. */
    @Bean
    public Queue eventsQueue() {
        return QueueBuilder.durable(EVENTS_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue eventsDeadLetterQueue() {
        return QueueBuilder.durable(EVENTS_DLQ).build();
    }

    @Bean
    public Binding playerEventsBinding(Queue eventsQueue, TopicExchange playerEventsExchange) {
        return BindingBuilder.bind(eventsQueue).to(playerEventsExchange).with(EVERY_EVENT);
    }

    @Bean
    public Binding purchaseEventsBinding(Queue eventsQueue, TopicExchange purchaseEventsExchange) {
        return BindingBuilder.bind(eventsQueue).to(purchaseEventsExchange).with(EVERY_EVENT);
    }

    @Bean
    public Binding itemEventsBinding(Queue eventsQueue, TopicExchange itemEventsExchange) {
        return BindingBuilder.bind(eventsQueue).to(itemEventsExchange).with(EVERY_EVENT);
    }

    /** A message that no retry can fix goes to the DLQ at once: one that does not convert, or one with no allow-list. */
    @Bean
    public RabbitListenerRetrySettingsCustomizer noRetryForFatalErrors() {
        FatalExceptionStrategy fatal = new ConditionalRejectingErrorHandler.DefaultExceptionStrategy() {
            @Override
            protected boolean isUserCauseFatal(Throwable cause) {
                return cause instanceof UnlistedEventException;
            }
        };
        return settings -> settings.setExceptionPredicate(exception -> !fatal.isFatal(exception));
    }

    /** Reads the JSON body into the listener's parameter type. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
