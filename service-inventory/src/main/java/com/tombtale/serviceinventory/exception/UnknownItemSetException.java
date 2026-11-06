package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * A template names an item set that does not exist. Answers 400.
 */
public class UnknownItemSetException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public UnknownItemSetException(UUID publicId) {
        super("Unknown item set: " + publicId);
    }
}
