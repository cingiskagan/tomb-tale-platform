package com.tombtale.servicecommerce.exception;

import java.io.Serial;

/**
 * Thrown when a requested purchase order cannot be found.
 */
public class PurchaseNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PurchaseNotFoundException(String message) {
        super(message);
    }
}
