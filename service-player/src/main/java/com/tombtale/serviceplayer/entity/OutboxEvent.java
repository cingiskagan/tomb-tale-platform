package com.tombtale.serviceplayer.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * One event waiting to be published, written in the transaction that produced it.
 * This row's {@code publicId} is the {@code eventId} of the published envelope.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "outbox")
public class OutboxEvent extends BaseEntity {

    /** Also the routing key, for example {@code player.created}. */
    @Column(nullable = false, updatable = false, length = 100)
    private String eventType;

    /** The shape of {@link #payload}. A breaking change raises it. */
    @Column(nullable = false, updatable = false)
    private int eventVersion;

    /** The {@code publicId} of the entity the event is about. */
    @Column(nullable = false, updatable = false)
    private UUID aggregateId;

    /** The envelope's data object. The other envelope fields are columns here. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    /** Null until the publisher sends the row. */
    private Instant publishedAt;
}
