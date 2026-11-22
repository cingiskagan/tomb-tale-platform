package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One line of a custom entry's bonus. Inventory checks the stat name. A value may pass the stat's roll range, so
 * testers can try extremes, but a line always adds.
 *
 * @param stat  the stat's name
 * @param value how much it adds, at least 1
 */
public record BonusLineRequest(
        @NotBlank(message = "a bonus line needs a stat")
        String stat,

        @NotNull(message = "a bonus line needs a value")
        @Positive(message = "a bonus value must be at least 1")
        Integer value
) {
}
