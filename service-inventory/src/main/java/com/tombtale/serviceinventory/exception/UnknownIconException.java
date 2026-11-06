package com.tombtale.serviceinventory.exception;

/**
 * A template names an icon hash that no upload had. Answers 400.
 */
public class UnknownIconException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param hash the hash the client sent */
    public UnknownIconException(String hash) {
        super("Unknown icon: " + hash);
    }
}
