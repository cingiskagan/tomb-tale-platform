package com.tombtale.commons.outbox;

import com.tombtale.commons.audit.SystemActor;

/**
 * The service an outbox belongs to. Each service with an outbox declares one as a bean.
 *
 * @param producer the service name that every envelope carries
 * @param exchange where the publisher sends the service's events
 * @param actor    who the publisher writes as when it marks a row published
 */
public record OutboxOwner(String producer, String exchange, SystemActor actor) {
}
