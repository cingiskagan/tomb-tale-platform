package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * Inventory knows no character with the {@code publicId} a client sent, or doesn't know it yet. Answers 404.
 */
public class CharacterNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public CharacterNotFoundException(UUID publicId) {
        super("Character not found with id: " + publicId);
    }
}
