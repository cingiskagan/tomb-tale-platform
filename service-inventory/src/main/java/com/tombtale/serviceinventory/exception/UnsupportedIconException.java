package com.tombtale.serviceinventory.exception;

/**
 * An upload that is neither PNG nor WebP by its first bytes, whatever its name says. Answers 415.
 */
public class UnsupportedIconException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates the exception with its fixed message. */
    public UnsupportedIconException() {
        super("An icon must be a PNG or WebP image");
    }
}
