package com.tombtale.serviceinventory.support;

import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.entity.ItemTemplate;

import java.util.Map;

/**
 * Templates for tests that care only about some of their fields.
 */
public final class ItemTemplates {

    private ItemTemplates() {
    }

    /**
     * @param code the template's code
     * @return a builder with every required field set, and a stack size of 1
     */
    public static ItemTemplate.ItemTemplateBuilder<?, ?> aTemplate(String code) {
        return ItemTemplate.builder()
                .code(code)
                .name(code)
                .itemType(ItemType.RING)
                .maxStackSize(ItemType.EQUIPMENT_STACK_SIZE)
                .price(0L)
                .baseStats(Map.of())
                .assetKey("Items/" + code);
    }
}
