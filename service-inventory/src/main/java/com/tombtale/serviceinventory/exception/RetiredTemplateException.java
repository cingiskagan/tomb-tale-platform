package com.tombtale.serviceinventory.exception;

/**
 * A stand entry names a retired template, which no stand shows. Answers 409.
 */
public class RetiredTemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param code the template's code */
    public RetiredTemplateException(String code) {
        super("The item template " + code + " is retired");
    }
}
