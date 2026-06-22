package com.tombtale.serviceplayer.dto;

import java.time.Instant;

/**
 * Read-only DTO returned to API consumers for player data.
 *
 * <p>
 * Intentionally omits the internal {@code zitadelUserId} to keep
 * the public contract clean and avoid leaking auth identifiers.
 *
 * @param id               unique player identifier
 * @param displayName      in-game display name
 * @param level            current player level
 * @param experiencePoints total XP accumulated
 * @param createdAt        UTC timestamp of account creation
 */
public record PlayerResponse(
        Long id,
        String displayName,
        int level,
        long experiencePoints,
        Instant createdAt) {
}
