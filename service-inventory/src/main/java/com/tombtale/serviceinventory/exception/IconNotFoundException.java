package com.tombtale.serviceinventory.exception;

/**
 * No upload had the hash a client asked for. Answers 404.
 */
public class IconNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param hash the hash the client sent */
    public IconNotFoundException(String hash) {
        super("Icon not found with hash: " + hash);
    }
}
