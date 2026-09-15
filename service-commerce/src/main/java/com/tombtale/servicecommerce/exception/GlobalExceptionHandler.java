package com.tombtale.servicecommerce.exception;

import com.tombtale.commons.web.PlatformExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Commerce's domain exceptions, on top of the shared error contract.
 *
 * <p>Cross-cutting failures — validation, lock conflicts, bad sort fields —
 * live in {@link PlatformExceptionHandler} instead. Every handler here logs
 * context before returning a client-safe message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex the not-found exception
     * @return a 404 problem detail
     */
    @ExceptionHandler(PurchaseNotFoundException.class)
    public ProblemDetail handlePurchaseNotFound(PurchaseNotFoundException ex) {
        LOG.warn("Purchase not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /**
     * A move the status machine does not allow, such as setting
     * {@code CANCELLED} through update instead of the soft-delete endpoint.
     *
     * @param ex the invalid transition exception
     * @return a 400 problem detail
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        LOG.warn("Invalid status transition: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid Status Transition", ex.getMessage());
    }
}
