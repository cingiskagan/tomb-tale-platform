package com.tombtale.serviceaudit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tombtale.serviceaudit.exception.UnlistedEventException;

class EventAllowListTest {

    @Test
    void keepsOnlyTheListedFields() {
        Map<String, Object> data = Map.of("purchasePublicId", "p-1", "reason", "UNKNOWN_ITEM_CODE", "note", "x");

        assertThat(EventAllowList.keep("item.grant-rejected", 1, data))
                .containsOnlyKeys("purchasePublicId", "reason");
    }

    @Test
    void aPurchaseKeepsTheCharacterThatReceivesIt() {
        Map<String, Object> data = Map.of("purchasePublicId", "p-1", "playerPublicId", "pl-1",
                "characterPublicId", "c-1", "itemCode", "SWORD_IRON", "quantity", 1);

        assertThat(EventAllowList.keep("purchase.created", 1, data)).isEqualTo(data);
    }

    @Test
    void aVersionWithNoListIsRefused() {
        assertThatThrownBy(() -> EventAllowList.keep("player.created", 2, Map.of()))
                .isInstanceOf(UnlistedEventException.class)
                .hasMessage("No allow-list for player.created v2");
    }
}
