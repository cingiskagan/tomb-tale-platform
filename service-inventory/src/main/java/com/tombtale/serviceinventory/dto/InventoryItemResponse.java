package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;

import java.util.Map;
import java.util.UUID;

/**
 * One item or stack a character owns, with what its template shows.
 *
 * @param publicId      the item's id, which equipping and moving name
 * @param code          its template's code
 * @param name          its template's name
 * @param itemType      its template's type, which decides the slot it fits
 * @param iconUrl       the path of its template's icon, or null for a generic one
 * @param rarity        its rarity
 * @param bonus         the stats it rolled
 * @param finalStats    its template's base stats plus its bonus
 * @param quantity      how many copies the stack holds
 * @param equipmentSlot its slot, or null in the bag
 * @param bagPosition   its cell, or null while equipped
 */
public record InventoryItemResponse(
        UUID publicId,
        String code,
        String name,
        ItemType itemType,
        String iconUrl,
        Rarity rarity,
        Map<Stat, Integer> bonus,
        Map<Stat, Integer> finalStats,
        Integer quantity,
        EquipmentSlot equipmentSlot,
        Integer bagPosition
) {
}
