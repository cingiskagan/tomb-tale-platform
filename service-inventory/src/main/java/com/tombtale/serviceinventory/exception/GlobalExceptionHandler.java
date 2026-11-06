package com.tombtale.serviceinventory.exception;

import com.tombtale.commons.web.PlatformExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Inventory's domain exceptions, on top of the shared error contract (ADR 0015).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * @param ex the missing template
     * @return a 404 problem detail
     */
    @ExceptionHandler(ItemTemplateNotFoundException.class)
    public ProblemDetail handleItemTemplateNotFound(ItemTemplateNotFoundException ex) {
        LOG.warn("Item template not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /**
     * @param ex the code that is taken
     * @return a 409 problem detail
     */
    @ExceptionHandler(DuplicateItemCodeException.class)
    public ProblemDetail handleDuplicateItemCode(DuplicateItemCodeException ex) {
        LOG.warn("Duplicate item code: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Duplicate Item Code", ex.getMessage());
    }

    /**
     * @param ex the stat name inventory does not know
     * @return a 400 problem detail
     */
    @ExceptionHandler(UnknownStatException.class)
    public ProblemDetail handleUnknownStat(UnknownStatException ex) {
        LOG.warn("Unknown stat: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Unknown Stat", ex.getMessage());
    }

    /**
     * @param ex the missing icon
     * @return a 404 problem detail
     */
    @ExceptionHandler(IconNotFoundException.class)
    public ProblemDetail handleIconNotFound(IconNotFoundException ex) {
        LOG.warn("Icon not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    /**
     * @param ex the icon hash a template names
     * @return a 400 problem detail
     */
    @ExceptionHandler(UnknownIconException.class)
    public ProblemDetail handleUnknownIcon(UnknownIconException ex) {
        LOG.warn("Unknown icon: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Unknown Icon", ex.getMessage());
    }

    /**
     * @param ex an upload that is neither PNG nor WebP
     * @return a 415 problem detail
     */
    @ExceptionHandler(UnsupportedIconException.class)
    public ProblemDetail handleUnsupportedIcon(UnsupportedIconException ex) {
        LOG.warn("Unsupported icon: {}", ex.getMessage());
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Icon", ex.getMessage());
    }

    /**
     * @param ex an upload over the limit
     * @return a 413 problem detail
     */
    @ExceptionHandler(IconTooLargeException.class)
    public ProblemDetail handleIconTooLarge(IconTooLargeException ex) {
        LOG.warn("Icon too large: {}", ex.getMessage());
        return problem(HttpStatus.CONTENT_TOO_LARGE, "Icon Too Large", ex.getMessage());
    }
}
