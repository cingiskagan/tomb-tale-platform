package com.tombtale.serviceplayer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Core Player entity stored in PostgreSQL.
 * <p>
 * The {@code zitadelUserId} links this game profile to the authenticated
 * identity managed by Zitadel (the "sub" claim in the JWT).
 */
@Data
@Builder
@AllArgsConstructor
@Entity
@Table(name = "players")
@EntityListeners(AuditingEntityListener.class)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Zitadel user ID — the "sub" claim from the JWT. Unique per player. */
    @Column(nullable = false, unique = true)
    private String zitadelUserId;

    /** In-game display name chosen by the player. */
    @Column(nullable = false, unique = true)
    private String displayName;

    /** Current player level. */
    @Column(nullable = false)
    private int level;

    /** Total experience points accumulated. */
    @Column(nullable = false)
    private long experiencePoints;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * No-args constructor required by JPA.
     * Sets sensible defaults for new player entities.
     */
    public Player() {
        this.level = 1;
        this.experiencePoints = 0;
    }
}
