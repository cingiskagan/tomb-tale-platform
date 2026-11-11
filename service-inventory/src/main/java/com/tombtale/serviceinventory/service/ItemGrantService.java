package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.BagCells;
import com.tombtale.serviceinventory.domain.ItemGrantOutcome;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.dto.event.PurchaseCreatedPayload;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemGrant;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.ItemGrantRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.repository.ItemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.UUID;

/**
 * Turns a purchase into an item grant: adds its items to the character's stacks and saves the item grant,
 * in one transaction.
 */
@Service
@RequiredArgsConstructor
public class ItemGrantService {

    private static final int SINGLE_CHARACTER = 1;
    private static final String NO_SINGLE_CHARACTER = "NO_SINGLE_CHARACTER";
    private static final String UNKNOWN_ITEM_CODE = "UNKNOWN_ITEM_CODE";
    private static final String RETIRED_ITEM_CODE = "RETIRED_ITEM_CODE";

    private final CharacterInventoryRepository characterInventoryRepository;
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

    /** Until the player can pick a character, the items go to their only one, locked first as every write does. */
    private ItemGrant createItemGrant(PurchaseCreatedPayload purchase) {
        List<UUID> characters = characterInventoryRepository.findPublicIdsByPlayerId(purchase.playerPublicId());
        if (characters.size() != SINGLE_CHARACTER) {
            return saveItemGrant(purchase, null, ItemGrantOutcome.REJECTED, NO_SINGLE_CHARACTER);
        }
        CharacterInventory character =
                characterInventoryRepository.findByPublicIdForUpdate(characters.getFirst()).orElseThrow();
        Optional<ItemTemplate> template = itemTemplateRepository.findByCode(purchase.itemCode());
        if (template.isEmpty()) {
            return saveItemGrant(purchase, character.getPublicId(), ItemGrantOutcome.REJECTED, UNKNOWN_ITEM_CODE);
        }
        if (template.get().isRetired()) {
            return saveItemGrant(purchase, character.getPublicId(), ItemGrantOutcome.REJECTED, RETIRED_ITEM_CODE);
        }
        addToStacks(character, template.get(), purchase.quantity());
        return saveItemGrant(purchase, character.getPublicId(), ItemGrantOutcome.GRANTED, null);
    }

    /**
     * A purchase grants gray copies with no bonus. They fill the stacks that have room, then open new ones in the
     * lowest free cells.
     */
    private void addToStacks(CharacterInventory character, ItemTemplate template, int quantity) {
        int remaining = quantity;
        for (ItemInstance stack : itemInstanceRepository.findStacksWithRoom(character, template, Rarity.GRAY)) {
            int added = Math.min(template.getMaxStackSize() - stack.getQuantity(), remaining);
            stack.setQuantity(stack.getQuantity() + added);
            remaining -= added;
            if (remaining == 0) {
                return;
            }
        }
        PrimitiveIterator.OfInt freeCells = BagCells.free(itemInstanceRepository.findBagPositions(character));
        while (remaining > 0) {
            int size = Math.min(template.getMaxStackSize(), remaining);
            itemInstanceRepository.save(ItemInstance.builder()
                    .characterInventory(character)
                    .template(template)
                    .quantity(size)
                    .bagPosition(freeCells.nextInt())
                    .build());
            remaining -= size;
        }
    }

    private ItemGrant saveItemGrant(
            PurchaseCreatedPayload purchase, UUID characterId, ItemGrantOutcome outcome, String reason) {
        return itemGrantRepository.save(ItemGrant.builder()
                .purchaseId(purchase.purchasePublicId())
                .playerId(purchase.playerPublicId())
                .characterId(characterId)
                .itemCode(purchase.itemCode())
                .quantity(purchase.quantity())
                .outcome(outcome)
                .reason(reason)
                .build());
    }
}
