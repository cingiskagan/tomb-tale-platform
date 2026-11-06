package com.tombtale.serviceinventory.exception;

/**
 * A new template names a code another template already has. Answers 409.
 */
public class DuplicateItemCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param code the code the client sent */
    public DuplicateItemCodeException(String code) {
        super("An item template already has the code " + code);
    }
}
