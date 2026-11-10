package com.tombtale.serviceinventory.exception;

import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.ItemType;

/**
 * The slot doesn't take the item's type, such as a ring on the head. Answers 400.
 */
public class WrongSlotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param slot the slot the client named
     * @param type the type of the item it named
     */
    public WrongSlotException(EquipmentSlot slot, ItemType type) {
        super(slot + " does not take " + type);
    }
}
