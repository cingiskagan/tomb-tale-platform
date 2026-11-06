package com.tombtale.serviceinventory.exception;

/**
 * A client named a stat that is not one of the 15. Answers 400.
 */
public class UnknownStatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param name the name the client sent */
    public UnknownStatException(String name) {
        super("Unknown stat: " + name);
    }
}
