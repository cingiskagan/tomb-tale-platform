package com.tombtale.serviceplayer.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only DTO returned to API consumers for player data.
 *
 * <p>
 * Intentionally omits the internal {@code id} and {@code zitadelUserId} to keep
 * the public contract clean and avoid leaking internal/auth identifiers.
 *
 * @param publicId         unique player public identifier
 * @param displayName      in-game display name
 * @param profileIcon      selected profile icon key
 * @param characters       list of characters owned by this player
 * @param createdAt        UTC timestamp of account creation
 */
public record PlayerResponse(
        UUID publicId,
        String displayName,
        String profileIcon,
        List<CharacterResponse> characters,
        Instant createdAt) {
}
