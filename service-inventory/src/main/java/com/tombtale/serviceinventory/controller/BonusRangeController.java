package com.tombtale.serviceinventory.controller;

import com.tombtale.commons.security.RoleConstants;
import com.tombtale.serviceinventory.domain.BonusPool;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Stat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The range each stat rolls in, per item type, which the portal shows beside a custom bonus. For
 * {@code platform_admin} only, like the custom items themselves.
 */
@RestController
@RequestMapping("/api/v1/inventory/bonus-ranges")
@PreAuthorize(RoleConstants.IS_ADMIN)
public class BonusRangeController {

    private static final Map<ItemType, Map<Stat, BonusPool.Range>> RANGES_BY_ITEM_TYPE =
            Arrays.stream(ItemType.values()).collect(Collectors.toMap(
                    Function.identity(),
                    type -> type.bonusPool().ranges(),
                    (first, second) -> first,
                    () -> new EnumMap<>(ItemType.class)));

    /**
     * @return each item type's stats, each with the lowest and highest value a roll gives; a stat missing never rolls
     */
    @GetMapping
    public Map<ItemType, Map<Stat, BonusPool.Range>> findBonusRanges() {
        return RANGES_BY_ITEM_TYPE;
    }
}
