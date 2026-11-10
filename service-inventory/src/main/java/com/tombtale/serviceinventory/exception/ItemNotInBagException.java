package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * Only an item in the bag can be equipped or moved, and this one is equipped. Answers 409.
 */
public class ItemNotInBagException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the item the client named */
    public ItemNotInBagException(UUID publicId) {
        super("Item " + publicId + " is equipped, not in the bag");
    }
}
