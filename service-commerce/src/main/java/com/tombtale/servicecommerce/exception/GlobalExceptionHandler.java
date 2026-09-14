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
 * <p>
 * Validation, optimistic-lock conflicts and rejected sort fields used to be
 * handled here too. They are cross-cutting, so they moved to
 * {@link PlatformExceptionHandler}, which both services extend. What is left
 * is what only this service can throw.
 *
 * <p>
 * The response shape changed with that move: RFC 9457
 * {@code application/problem+json} instead of the hand-rolled
 * {@code ErrorResponse} record, so {@code message} is now {@code detail} and
 * {@code error} is now {@code title}.
 *
 * <p>
 * Every handler logs context before returning a client-safe message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing purchase lookups.
     *
     * @param ex the not-found exception
     * @return a 404 problem detail
     */
    @ExceptionHandler(PurchaseNotFoundException.class)
    public ProblemDetail handlePurchaseNotFound(PurchaseNotFoundException ex) {
        LOG.warn("Purchase not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /**
     * Handles a move the purchase status machine does not allow, such as
     * setting {@code CANCELLED} through the update endpoint instead of the
     * dedicated soft-delete.
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
