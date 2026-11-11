package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * The character owns no item with the {@code publicId} a client sent. Answers 404.
 */
public class ItemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public ItemNotFoundException(UUID publicId) {
        super("The character has no item with id: " + publicId);
    }
}
