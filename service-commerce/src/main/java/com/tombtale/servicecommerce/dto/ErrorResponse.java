package com.tombtale.servicecommerce.dto;

import java.time.Instant;

/**
 * Standardised error payload returned by {@code GlobalExceptionHandler}.
 *
 * @param status    HTTP status code (e.g. 404)
 * @param error     short error label (e.g. "Not Found")
 * @param message   human-readable details about the failure
 * @param timestamp UTC instant when the error occurred
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {
}
