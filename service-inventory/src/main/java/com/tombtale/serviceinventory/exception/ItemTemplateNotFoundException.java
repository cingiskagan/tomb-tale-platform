package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * No template carries the {@code publicId} a client sent. Answers 404.
 */
public class ItemTemplateNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public ItemTemplateNotFoundException(UUID publicId) {
        super("Item template not found with id: " + publicId);
    }
}
