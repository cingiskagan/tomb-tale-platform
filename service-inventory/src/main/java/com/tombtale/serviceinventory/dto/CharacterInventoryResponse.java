package com.tombtale.serviceinventory.dto;

import com.tombtale.serviceinventory.domain.EquipmentSlot;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A character's inventory as the portal and the game show it, computed at read time.
 *
 * @param characterId the character's {@code publicId}
 * @param coins       its coins
 * @param equipment   its equipped items by slot. An empty slot has no entry
 * @param bag         the items in its bag, lowest cell first
 * @param setBonuses  the sets whose every template it has equipped
 */
public record CharacterInventoryResponse(
        UUID characterId,
        Long coins,
        Map<EquipmentSlot, InventoryItemResponse> equipment,
        List<InventoryItemResponse> bag,
        List<SetBonusResponse> setBonuses
) {
}
