package com.tombtale.servicecommerce.repository;

import com.tombtale.servicecommerce.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link OutboxEvent} entities.
 * <p>
 * Used by the outbox poller to find unpublished events and
 * mark them as published after successful broker delivery.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Finds all events that have not yet been published to the broker.
     * The poller calls this on each tick and processes the results.
     */
    List<OutboxEvent> findByPublishedFalse();
}
