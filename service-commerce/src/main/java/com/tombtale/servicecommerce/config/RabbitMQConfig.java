package com.tombtale.servicecommerce.config;

import com.tombtale.servicecommerce.dto.event.ItemGrantRejectedPayload;
import com.tombtale.servicecommerce.dto.event.ItemGrantedPayload;
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
 * Commerce's own exchange, and its queues on service-player's and service-inventory's exchanges, each with a DLQ.
 * Every exchange is declared on both sides, so either service can start first.
 */
@Configuration
@SuppressWarnings("PMD.TooManyMethods") // one bean per broker object, and each queue brings three
public class RabbitMQConfig {

    public static final String PLAYER_EVENTS_EXCHANGE = "player.events";
    public static final String PLAYER_CREATED_QUEUE = "commerce.player-created";
    public static final String PLAYER_CREATED_DLQ = PLAYER_CREATED_QUEUE + ".dlq";

    public static final String PURCHASE_EVENTS_EXCHANGE = "purchase.events";

    public static final String ITEM_EVENTS_EXCHANGE = "item.events";
    public static final String ITEM_GRANTED_QUEUE = "commerce.item-granted";
    public static final String ITEM_GRANTED_DLQ = ITEM_GRANTED_QUEUE + ".dlq";
    public static final String ITEM_GRANT_REJECTED_QUEUE = "commerce.item-grant-rejected";
    public static final String ITEM_GRANT_REJECTED_DLQ = ITEM_GRANT_REJECTED_QUEUE + ".dlq";

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

    /** Where the outbox publisher sends {@code purchase.created}. service-inventory declares it the same way. */
    @Bean
    public TopicExchange purchaseEventsExchange() {
        return new TopicExchange(PURCHASE_EVENTS_EXCHANGE, true, false);
    }

    /** Declared with the same arguments as service-inventory's, or the broker refuses one of them. */
    @Bean
    public TopicExchange itemEventsExchange() {
        return new TopicExchange(ITEM_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue itemGrantedQueue() {
        return QueueBuilder.durable(ITEM_GRANTED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(ITEM_GRANTED_DLQ)
                .build();
    }

    @Bean
    public Queue itemGrantedDeadLetterQueue() {
        return QueueBuilder.durable(ITEM_GRANTED_DLQ).build();
    }

    @Bean
    public Binding itemGrantedBinding(Queue itemGrantedQueue, TopicExchange itemEventsExchange) {
        return BindingBuilder.bind(itemGrantedQueue).to(itemEventsExchange).with(ItemGrantedPayload.EVENT_TYPE);
    }

    @Bean
    public Queue itemGrantRejectedQueue() {
        return QueueBuilder.durable(ITEM_GRANT_REJECTED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(ITEM_GRANT_REJECTED_DLQ)
                .build();
    }

    @Bean
    public Queue itemGrantRejectedDeadLetterQueue() {
        return QueueBuilder.durable(ITEM_GRANT_REJECTED_DLQ).build();
    }

    @Bean
    public Binding itemGrantRejectedBinding(Queue itemGrantRejectedQueue, TopicExchange itemEventsExchange) {
        return BindingBuilder.bind(itemGrantRejectedQueue).to(itemEventsExchange).with(ItemGrantRejectedPayload.EVENT_TYPE);
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
