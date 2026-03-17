package com.tombtale.servicecommerce.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Commerce Service.
 * <p>
 * Declares the commerce events topic exchange.
 * Downstream consumers (inventory, notification, analytics) bind their
 * queues to this exchange with routing keys like:
 * <ul>
 *     <li>{@code purchase.created}</li>
 *     <li>{@code purchase.cancelled}</li>
 *     <li>{@code purchase.refunded}</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    public static final String COMMERCE_EVENTS_EXCHANGE = "commerce.events";

    /**
     * Topic exchange for commerce-related events.
     * Durable and non-auto-delete so it survives broker restarts.
     */
    @Bean
    public TopicExchange commerceEventsExchange() {
        return new TopicExchange(COMMERCE_EVENTS_EXCHANGE, true, false);
    }

    /**
     * JSON message converter for RabbitMQ payloads.
     * Enables sending and receiving POJOs as JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
