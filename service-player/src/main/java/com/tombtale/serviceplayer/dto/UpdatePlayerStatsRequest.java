package com.tombtale.serviceplayer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for strictly updating a player's core progression stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlayerStatsRequest {

    @NotNull(message = "Level is required")
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 100, message = "Level cannot exceed 100")
    private Integer level;

    @NotNull(message = "Experience points are required")
    @Min(value = 0, message = "Experience points cannot be negative")
    @Max(value = 1_000_000_000, message = "Experience points cannot exceed 1,000,000,000")
    private Long experiencePoints;
}
