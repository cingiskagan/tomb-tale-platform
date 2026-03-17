package com.tombtale.servicecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox event persisted in the same DB transaction as the
 * business operation. A scheduled poller reads unpublished rows and
 * forwards them to RabbitMQ, guaranteeing at-least-once delivery even
 * when the broker is temporarily unavailable.
 */
@Entity
@Table(name = "outbox_events")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** ID of the aggregate root this event belongs to (e.g., purchase order ID). */
    @Column(nullable = false)
    private String aggregateId;

    /** Routing key for RabbitMQ (e.g., "purchase.completed"). */
    @Column(nullable = false)
    private String eventType;

    /** JSON-serialised event payload. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Whether this event has been successfully published to the broker. */
    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
