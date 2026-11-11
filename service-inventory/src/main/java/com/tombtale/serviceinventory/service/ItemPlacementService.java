package com.tombtale.serviceinventory.service;

import com.tombtale.serviceinventory.domain.BagCells;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.dto.CharacterInventoryResponse;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.exception.CellOutsideBagException;
import com.tombtale.serviceinventory.exception.CharacterNotFoundException;
import com.tombtale.serviceinventory.exception.ItemNotFoundException;
import com.tombtale.serviceinventory.exception.ItemNotInBagException;
import com.tombtale.serviceinventory.exception.WrongSlotException;
import com.tombtale.serviceinventory.repository.CharacterInventoryRepository;
import com.tombtale.serviceinventory.repository.ItemInstanceRepository;
import com.tombtale.serviceinventory.security.InventoryCaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Moves a character's items between its slots and bag cells. Each change locks the character first, then works
 * on the items it loaded, so no query runs while an item is halfway between a slot and a cell.
 */
@Service
public class ItemPlacementService {

    private final CharacterInventoryRepository characterInventoryRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final InventoryView inventoryView;
    private final int bagCells;

    /** The bag's size comes from {@code inventory.bag-cells} in application.yml. */
    public ItemPlacementService(
            CharacterInventoryRepository characterInventoryRepository,
            ItemInstanceRepository itemInstanceRepository,
            InventoryView inventoryView,
            @Value("${inventory.bag-cells}") int bagCells) {
        this.characterInventoryRepository = characterInventoryRepository;
        this.itemInstanceRepository = itemInstanceRepository;
        this.inventoryView = inventoryView;
        this.bagCells = bagCells;
    }

    /**
     * Steps 1 to 4 of the design. Step 5, telling the game server, waits until there is one.
     *
     * @param characterId the character
     * @param slot        the slot to equip
     * @param itemId      an item in the character's bag
     * @param caller      who asks
     * @return the inventory after the change
     * @throws ItemNotInBagException if the item is equipped in another slot
     * @throws WrongSlotException    if the slot doesn't take the item's type
     */
    @Transactional
    public CharacterInventoryResponse equip(UUID characterId, EquipmentSlot slot, UUID itemId, InventoryCaller caller) {
        CharacterInventory character = lock(characterId, caller);
        List<ItemInstance> items = itemInstanceRepository.findByCharacterInventory(character);
        ItemInstance item = find(items, itemId);
        if (item.getEquipmentSlot() != slot) {
            if (item.getBagPosition() == null) {
                throw new ItemNotInBagException(itemId);
            }
            ItemType type = item.getTemplate().getItemType();
            if (!slot.takes(type)) {
                throw new WrongSlotException(slot, type);
            }
            List<ItemInstance> leaving = Stream.of(inSlot(items, slot), blockedBy(type, items))
                    .filter(Objects::nonNull)
                    .toList();
            int freedCell = item.getBagPosition();
            item.setBagPosition(null);
            item.setEquipmentSlot(slot);
            sendToBag(leaving, freedCell, items);
        }
        return inventoryView.build(character, items);
    }

    /**
     * Sends the item in the slot to the lowest free cell. An empty slot stays empty.
     *
     * @param characterId the character
     * @param slot        the slot to empty
     * @param caller      who asks
     * @return the inventory after the change
     */
    @Transactional
    public CharacterInventoryResponse unequip(UUID characterId, EquipmentSlot slot, InventoryCaller caller) {
        CharacterInventory character = lock(characterId, caller);
        List<ItemInstance> items = itemInstanceRepository.findByCharacterInventory(character);
        ItemInstance item = inSlot(items, slot);
        if (item != null) {
            item.setEquipmentSlot(null);
            item.setBagPosition(lowestFreeCell(items));
        }
        return inventoryView.build(character, items);
    }

    /**
     * Moves a bag item to a cell, and the item already there, if any, to the cell it left.
     *
     * @param characterId the character
     * @param cell        the cell to move to
     * @param itemId      an item in the character's bag
     * @param caller      who asks
     * @return the inventory after the change
     * @throws CellOutsideBagException if the cell is past the bag's last one
     * @throws ItemNotInBagException   if the item is equipped
     */
    @Transactional
    public CharacterInventoryResponse move(UUID characterId, int cell, UUID itemId, InventoryCaller caller) {
        if (cell >= bagCells) {
            throw new CellOutsideBagException(cell, bagCells);
        }
        CharacterInventory character = lock(characterId, caller);
        List<ItemInstance> items = itemInstanceRepository.findByCharacterInventory(character);
        ItemInstance item = find(items, itemId);
        Integer from = item.getBagPosition();
        if (from == null) {
            throw new ItemNotInBagException(itemId);
        }
        items.stream()
                .filter(other -> Objects.equals(other.getBagPosition(), cell))
                .findFirst()
                .ifPresent(other -> other.setBagPosition(from));
        item.setBagPosition(cell);
        return inventoryView.build(character, items);
    }

    private CharacterInventory lock(UUID characterId, InventoryCaller caller) {
        CharacterInventory character = characterInventoryRepository.findByPublicIdForUpdate(characterId)
                .orElseThrow(() -> new CharacterNotFoundException(characterId));
        caller.checkMayActOn(character);
        return character;
    }

    private static ItemInstance find(List<ItemInstance> items, UUID itemId) {
        return items.stream()
                .filter(item -> item.getPublicId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    private static ItemInstance inSlot(List<ItemInstance> items, EquipmentSlot slot) {
        return items.stream().filter(item -> item.getEquipmentSlot() == slot).findFirst().orElse(null);
    }

    /** Step 4: a two-handed weapon needs the off hand, so it and a shield send each other to the bag. */
    private static ItemInstance blockedBy(ItemType type, List<ItemInstance> items) {
        if (type == ItemType.TWO_HANDED_WEAPON) {
            return inSlot(items, EquipmentSlot.OFF_HAND);
        }
        if (type == ItemType.SHIELD) {
            ItemInstance mainHand = inSlot(items, EquipmentSlot.MAIN_HAND);
            if (mainHand != null && mainHand.getTemplate().getItemType() == ItemType.TWO_HANDED_WEAPON) {
                return mainHand;
            }
        }
        return null;
    }

    /** The first item to leave takes the cell the equipped item left, and any other the lowest free cell. */
    private static void sendToBag(List<ItemInstance> leaving, int freedCell, List<ItemInstance> items) {
        for (int i = 0; i < leaving.size(); i++) {
            ItemInstance item = leaving.get(i);
            item.setEquipmentSlot(null);
            item.setBagPosition(i == 0 ? freedCell : lowestFreeCell(items));
        }
    }

    private static int lowestFreeCell(List<ItemInstance> items) {
        return BagCells.free(items.stream().map(ItemInstance::getBagPosition).filter(Objects::nonNull).toList())
                .nextInt();
    }
}
