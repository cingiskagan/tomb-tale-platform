package com.tombtale.serviceplayer.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only DTO for character data.
 */
public record CharacterResponse(
        UUID publicId,
        String name,
        int level,
        long experiencePoints,
        Instant createdAt) {
}
