package com.tombtale.serviceplayer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Core Player document stored in MongoDB.
 * <p>
 * The {@code zitadelUserId} links this game profile to the authenticated
 * identity managed by Zitadel (the "sub" claim in the JWT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private String id;

    /** Zitadel user ID — the "sub" claim from the JWT. Unique per player. */
    @Indexed(unique = true)
    private String zitadelUserId;

    /** In-game display name chosen by the player. */
    @Indexed(unique = true)
    private String displayName;

    /** Current player level. */
    @Builder.Default
    private int level = 1;

    /** Total experience points accumulated. */
    @Builder.Default
    private long experiencePoints = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
