package com.tombtale.serviceinventory.exception;

/**
 * A new template, item set or dealer names a code another one already has. Answers 409.
 */
public class DuplicateCodeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param kind what the code names, such as {@code item template}
     * @param code the code the client sent
     */
    public DuplicateCodeException(String kind, String code) {
        super("Another " + kind + " already has the code " + code);
    }
}
