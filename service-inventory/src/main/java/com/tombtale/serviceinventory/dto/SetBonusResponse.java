package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.Stat;

import java.util.Map;

/**
 * A set the character has complete, and what it adds.
 *
 * @param code  the set's code
 * @param name  the set's name
 * @param bonus the stats the complete set adds
 */
public record SetBonusResponse(String code, String name, Map<Stat, Integer> bonus) {
}
