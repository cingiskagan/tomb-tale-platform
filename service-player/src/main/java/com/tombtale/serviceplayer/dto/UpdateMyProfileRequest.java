package com.tombtale.serviceplayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for a player updating their own profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMyProfileRequest {

    @NotBlank(message = "Display name cannot be blank")
    @Size(min = 3, max = 30, message = "Display name must be between 3 and 30 characters")
    private String displayName;

    private String profileIcon;
}
