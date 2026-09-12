package com.tombtale.commons.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * The six fields every entity on the platform carries.
 *
 * <p>{@code id} is the internal key and never leaves the persistence layer.
 * {@code publicId} is the only identifier that appears in an API or crosses a
 * service boundary. The four audit fields record when a row changed and who
 * changed it.
 *
 * <p>{@code publicId} is assigned where it is declared rather than in a
 * {@code @PrePersist} callback, so it is already set on an object that has
 * never been saved. Equality depends on it, and equality has to work before
 * the first flush — an entity put in a {@code Set} must still be findable
 * there after it is persisted.
 *
 * <p>A row inserted by raw SQL never runs this code, so every table also
 * declares {@code DEFAULT gen_random_uuid()} on the column.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // shared state, not shared behaviour
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing ID, used in APIs to prevent sequential ID enumeration.
     *
     * <p>No public setter. The column is {@code updatable = false}, so a write
     * here would never reach the row, and {@code equals}/{@code hashCode} read
     * it — changing it on a persisted entity silently desynchronises the object
     * from its row and loses it from any hash-based collection holding it. Set
     * it through the builder, at construction, or not at all.
     */
    @Column(nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.PACKAGE)
    @Builder.Default
    private UUID publicId = UUID.randomUUID();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * The principal that created this row: a player's {@code publicId}, or a
     * {@link com.tombtale.commons.audit.SystemActor} id for a writer that is
     * not a person.
     *
     * <p>Nullable only until every service can resolve its caller. Once that
     * lands, a write nobody can be credited with is rejected instead.
     */
    @CreatedBy
    @Column(updatable = false)
    private UUID createdBy;

    /** The principal that last modified this row. See {@link #createdBy}. */
    @LastModifiedBy
    private UUID updatedBy;

    /**
     * Entities are equal when they carry the same {@code publicId}.
     *
     * <p>Deliberately not the generated primary key: {@code id} is null until
     * the row is inserted, so a key-based equals makes an unsaved entity equal
     * to every other unsaved entity, and changes an object's hash code mid-
     * transaction. {@code publicId} exists from construction and never changes.
     *
     * <p>Compared with {@code instanceof} rather than {@code getClass()},
     * because a lazily loaded entity is a proxy subclass and would otherwise
     * never equal the entity it stands for.
     *
     * @param other the object to compare with
     * @return true when both are entities sharing one {@code publicId}
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BaseEntity that)) {
            return false;
        }
        return publicId != null && publicId.equals(that.getPublicId());
    }

    /**
     * Hashes the {@code publicId}, which is fixed for the life of the object.
     *
     * @return the hash code of this entity
     */
    @Override
    public final int hashCode() {
        return publicId == null ? 0 : publicId.hashCode();
    }
}
