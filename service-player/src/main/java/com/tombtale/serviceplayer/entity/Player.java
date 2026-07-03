package com.tombtale.serviceplayer.entity;

import jakarta.persistence.Column;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

    /** Public-facing ID, used in APIs to prevent sequential ID enumeration. */
    @Column(nullable = false, unique = true, updatable = false)
    @ColumnDefault("gen_random_uuid()")
    private UUID publicId;

    /** Zitadel user ID — the "sub" claim from the JWT. Unique per player. */
    @Column(nullable = false, unique = true)
    private String zitadelUserId;

    /** In-game display name chosen by the player. */
    @Column(nullable = false, unique = true)
    private String displayName;

    /** Selected profile icon key (e.g., pi-user) */
    @Column(nullable = false)
    @ColumnDefault("'pi-user'")
    @Builder.Default
    private String profileIcon = "pi-user";

    /** Characters owned by this player. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<GameCharacter> characters = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Lifecycle callback invoked before the entity is persisted.
     * Generates a random UUID for the public ID if it has not been set.
     */
    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    /**
     * Returns an unmodifiable view of the characters list.
     * @return unmodifiable list of characters
     */
    public List<GameCharacter> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    /**
     * Adds a character to this player.
     * @param character the character to add
     */
    public void addCharacter(GameCharacter character) {
        characters.add(character);
        character.setPlayer(this);
    }

    /**
     * Removes a character from this player.
     * @param character the character to remove
     */
    public void removeCharacter(GameCharacter character) {
        characters.remove(character);
        character.setPlayer(null);
    }
}
