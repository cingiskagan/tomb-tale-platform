package com.tombtale.serviceplayer.entity;

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
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
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
    @Builder.Default
    private int level = 1;

    /** Total experience points accumulated. */
    @Column(nullable = false)
    @Builder.Default
    private long experiencePoints = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
