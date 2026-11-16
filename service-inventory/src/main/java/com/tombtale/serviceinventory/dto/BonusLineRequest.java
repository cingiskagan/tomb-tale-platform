package com.tombtale.serviceinventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One line of a custom entry's bonus. Inventory checks the stat name.
 *
 * @param stat  the stat's name
 * @param value how much it adds
 */
public record BonusLineRequest(
        @NotBlank(message = "a bonus line needs a stat")
        String stat,

        @NotNull(message = "a bonus line needs a value")
        Integer value
) {
}
