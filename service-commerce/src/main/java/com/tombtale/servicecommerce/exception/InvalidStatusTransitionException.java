package com.tombtale.servicecommerce.exception;

/**
 * Thrown when a client attempts an invalid status transition on a purchase.
 *
 * <p>For example, setting status to {@code CANCELLED} via the update endpoint
 * instead of using the dedicated soft-delete (DELETE) endpoint.
 *
 * <p>Caught by {@code GlobalExceptionHandler} and mapped to HTTP 400.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception for a rejected status transition.
     *
     * @param message explanation of why the transition is not allowed
     */
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
