package com.tombtale.serviceinventory.exception;

/**
 * No dealer carries the {@code publicId} or the code a client sent. Answers 404.
 */
public class DealerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param key the id or the code the client sent */
    public DealerNotFoundException(Object key) {
        super("Dealer not found: " + key);
    }
}
