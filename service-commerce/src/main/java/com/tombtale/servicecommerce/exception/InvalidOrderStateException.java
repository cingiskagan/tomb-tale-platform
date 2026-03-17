package com.tombtale.servicecommerce.exception;

import java.io.Serial;

/**
 * Thrown when a purchase order status transition is not allowed.
 * For example, cancelling an already-completed order.
 */
public class InvalidOrderStateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
