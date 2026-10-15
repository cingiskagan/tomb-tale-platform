package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.ItemGrantOutcome;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Turns a purchase into an item grant: adds its items to the player's stacks and saves the item grant,
 * in one transaction.
 */
@Service
@RequiredArgsConstructor
public class ItemGrantService {

    private static final String UNKNOWN_ITEM_CODE = "UNKNOWN_ITEM_CODE";

    private final ItemTemplateRepository itemTemplateRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final ItemGrantRepository itemGrantRepository;

    /**
     * Creates one item grant per purchase. A redelivered purchase gets its saved item grant back and adds nothing.
     *
     * @param purchase the data of the delivered {@code purchase.created}
     * @return the item grant to send to commerce
     */
    @Transactional
    public ItemGrant findOrCreateItemGrant(PurchaseCreatedPayload purchase) {
        return itemGrantRepository.findByPurchaseId(purchase.purchasePublicId())
                .orElseGet(() -> createItemGrant(purchase));
    }

    private ItemGrant createItemGrant(PurchaseCreatedPayload purchase) {
        Optional<ItemTemplate> template = itemTemplateRepository.findByCode(purchase.itemCode());
        if (template.isEmpty()) {
            return saveItemGrant(purchase, ItemGrantOutcome.REJECTED, UNKNOWN_ITEM_CODE);
        }
        addToStacks(purchase.playerPublicId(), template.get(), purchase.quantity());
        return saveItemGrant(purchase, ItemGrantOutcome.GRANTED, null);
    }

    /** Fills the stacks that have room, oldest first, then opens new stacks for the rest (data-store-research.md). */
    private void addToStacks(UUID playerId, ItemTemplate template, int quantity) {
        int remaining = quantity;
        for (ItemInstance stack : itemInstanceRepository.findStacksWithRoom(playerId, template)) {
            int added = Math.min(template.getMaxStackSize() - stack.getQuantity(), remaining);
            stack.setQuantity(stack.getQuantity() + added);
            remaining -= added;
            if (remaining == 0) {
                return;
            }
        }
        while (remaining > 0) {
            int size = Math.min(template.getMaxStackSize(), remaining);
            itemInstanceRepository.save(ItemInstance.builder()
                    .playerId(playerId)
                    .template(template)
                    .quantity(size)
                    .build());
            remaining -= size;
        }
    }

    private ItemGrant saveItemGrant(PurchaseCreatedPayload purchase, ItemGrantOutcome outcome, String reason) {
        return itemGrantRepository.save(ItemGrant.builder()
                .purchaseId(purchase.purchasePublicId())
                .playerId(purchase.playerPublicId())
                .itemCode(purchase.itemCode())
                .quantity(purchase.quantity())
                .outcome(outcome)
                .reason(reason)
                .build());
    }
}
