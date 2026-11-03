package com.tombtale.serviceinventory.config;

import com.tombtale.serviceinventory.dto.event.PlayerCreatedPayload;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
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
 * Inventory's queues on service-player's and commerce's exchanges, each with a DLQ, and the exchange inventory
 * sends item grants to. Every exchange is declared on both sides, so either service can start first.
 */
@Configuration
@SuppressWarnings("PMD.TooManyMethods") // one bean per broker object, and each queue brings three
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";
    public static final String PLAYER_CREATED_QUEUE = "inventory.player-created";
    public static final String PLAYER_CREATED_DLQ = PLAYER_CREATED_QUEUE + ".dlq";

    public static final String PURCHASE_EVENTS_EXCHANGE = "purchase.events";
    public static final String PURCHASE_CREATED_QUEUE = "inventory.purchase-created";
    public static final String PURCHASE_CREATED_DLQ = PURCHASE_CREATED_QUEUE + ".dlq";

    public static final String ITEM_EVENTS_EXCHANGE = "item.events";

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

    /** Declared with the same arguments as service-commerce's, or the broker refuses one of them. */
    @Bean
    public TopicExchange purchaseEventsExchange() {
        return new TopicExchange(PURCHASE_EVENTS_EXCHANGE, true, false);
    }

    /** A rejected message goes through the default exchange, which routes by queue name, to the DLQ. */
    @Bean
    public Queue purchaseCreatedQueue() {
        return QueueBuilder.durable(PURCHASE_CREATED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(PURCHASE_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue purchaseCreatedDeadLetterQueue() {
        return QueueBuilder.durable(PURCHASE_CREATED_DLQ).build();
    }

    @Bean
    public Binding purchaseCreatedBinding(Queue purchaseCreatedQueue, TopicExchange purchaseEventsExchange) {
        return BindingBuilder.bind(purchaseCreatedQueue).to(purchaseEventsExchange).with(PurchaseCreatedPayload.EVENT_TYPE);
    }

    /** Where inventory sends its item grants. service-commerce declares it the same way. */
    @Bean
    public TopicExchange itemEventsExchange() {
        return new TopicExchange(ITEM_EVENTS_EXCHANGE, true, false);
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
