package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.controller.ItemIconController;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.Stat;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.dto.InventoryItemResponse;
import com.tombtale.serviceinventory.dto.SetBonusResponse;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemSet;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the inventory a read answers with. Final stats and set bonuses come from the templates as they are now,
 * so a template's edit reaches every copy without a migration.
 */
@Component
@RequiredArgsConstructor
public class InventoryView {

    private final ItemTemplateRepository itemTemplateRepository;

    /**
     * @param character the character
     * @param items     all of its items, with their templates
     * @return the character's inventory
     */
    public CharacterInventoryResponse build(CharacterInventory character, List<ItemInstance> items) {
        Map<EquipmentSlot, InventoryItemResponse> equipment = items.stream()
                .filter(item -> item.getEquipmentSlot() != null)
                .collect(Collectors.toMap(ItemInstance::getEquipmentSlot, InventoryView::toResponse,
                        (first, second) -> first, () -> new EnumMap<>(EquipmentSlot.class)));
        List<InventoryItemResponse> bag = items.stream()
                .filter(item -> item.getBagPosition() != null)
                .sorted(Comparator.comparing(ItemInstance::getBagPosition))
                .map(InventoryView::toResponse)
                .toList();
        Set<ItemTemplate> equipped = items.stream()
                .filter(item -> item.getEquipmentSlot() != null)
                .map(ItemInstance::getTemplate)
                .collect(Collectors.toSet());
        return new CharacterInventoryResponse(
                character.getPublicId(), character.getCoins(), equipment, bag, fullSets(equipped));
    }

    /** A set is full while every template in it is equipped. */
    private List<SetBonusResponse> fullSets(Set<ItemTemplate> equipped) {
        Set<ItemSet> sets = equipped.stream()
                .map(ItemTemplate::getItemSet)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sets.isEmpty()) {
            return List.of();
        }
        return itemTemplateRepository.findByItemSetIn(sets).stream()
                .collect(Collectors.groupingBy(ItemTemplate::getItemSet))
                .entrySet().stream()
                .filter(set -> equipped.containsAll(set.getValue()))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(ItemSet::getCode))
                .map(set -> new SetBonusResponse(set.getCode(), set.getName(), set.getBonus()))
                .toList();
    }

    private static InventoryItemResponse toResponse(ItemInstance item) {
        ItemTemplate template = item.getTemplate();
        return new InventoryItemResponse(
                item.getPublicId(),
                template.getCode(),
                template.getName(),
                template.getItemType(),
                template.getIconHash() == null ? null : ItemIconController.PATH + "/" + template.getIconHash(),
                item.getRarity(),
                item.getBonus(),
                Stat.sum(List.of(template.getBaseStats(), item.getBonus())),
                item.getQuantity(),
                item.getEquipmentSlot(),
                item.getBagPosition());
    }
}
