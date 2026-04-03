package com.tombtale.servicecommerce.exception;

import com.tombtale.servicecommerce.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised exception handler that translates domain and framework
 * exceptions into structured {@link ErrorResponse} payloads.
 *
 * <p>
 * Every handler logs context before returning a client-safe message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
        private static final String OPTIMISTIC_LOCK_MESSAGE =
                "The resource was modified by another transaction. Please retry.";

        /**
         * Handles missing purchase lookups.
         *
         * @param ex the not-found exception
         * @return 404 error response
         */
        @ExceptionHandler(PurchaseNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ErrorResponse handlePurchaseNotFound(PurchaseNotFoundException ex) {
                LOG.warn("Purchase not found: {}", ex.getMessage());
                return new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "Not Found",
                                ex.getMessage(),
                                Instant.now());
        }

        /**
         * Handles Jakarta Validation failures on request bodies.
         *
         * @param ex the validation exception containing field errors
         * @return 400 error response with concatenated field messages
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
                String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                LOG.warn("Validation failed: {}", fieldErrors);
                return new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Validation Error",
                                fieldErrors,
                                Instant.now());
        }

        /**
         * Handles optimistic lock conflicts caused by concurrent updates.
         *
         * @param ex the optimistic locking exception
         * @return 409 error response advising the client to retry
         */
        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        public ErrorResponse handleOptimisticLockConflict(ObjectOptimisticLockingFailureException ex) {
                LOG.warn("Optimistic lock conflict: {}", ex.getMessage());
                return new ErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "Conflict",
                                OPTIMISTIC_LOCK_MESSAGE,
                                Instant.now());
        }

        /**
         * Handles invalid status transition attempts (e.g., setting CANCELLED via
         * update).
         *
         * @param ex the invalid transition exception
         * @return 400 error response
         */
        @ExceptionHandler(InvalidStatusTransitionException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ErrorResponse handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
                LOG.warn("Invalid status transition: {}", ex.getMessage());
                return new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "Invalid Status Transition",
                                ex.getMessage(),
                                Instant.now());
        }
}
