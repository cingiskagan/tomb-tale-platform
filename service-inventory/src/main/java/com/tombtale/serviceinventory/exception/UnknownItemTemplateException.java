package com.tombtale.serviceinventory.exception;

import java.util.UUID;

/**
 * A stand entry names a template that does not exist. Answers 400.
 */
public class UnknownItemTemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param publicId the id the client sent */
    public UnknownItemTemplateException(UUID publicId) {
        super("Unknown item template: " + publicId);
    }
}
