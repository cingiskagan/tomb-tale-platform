package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * No item set carries the {@code publicId} a client sent. Answers 404.
 */
public class ItemSetNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public ItemSetNotFoundException(UUID publicId) {
        super("Item set not found with id: " + publicId);
    }
}
