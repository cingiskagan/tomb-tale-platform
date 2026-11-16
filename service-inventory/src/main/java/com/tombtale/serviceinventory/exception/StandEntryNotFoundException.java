package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * The dealer's stand has no entry with the {@code publicId} a client sent. Answers 404.
 */
public class StandEntryNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public StandEntryNotFoundException(UUID publicId) {
        super("Stand entry not found with id: " + publicId);
    }
}
