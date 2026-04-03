package com.tombtale.servicecommerce.exception;

import java.util.UUID;

/**
 * Thrown when a purchase lookup by ID yields no result.
 *
 * <p>Caught by {@code GlobalExceptionHandler} and mapped to HTTP 404.
 */
public class PurchaseNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception indicating the given purchase ID was not found.
     *
     * @param purchaseId the UUID that could not be resolved
     */
    public PurchaseNotFoundException(UUID purchaseId) {
        super("Purchase not found with id: " + purchaseId);
    }
}
