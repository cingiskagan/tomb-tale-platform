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
    private static final String NOT_FOUND = "Not Found";

    /**
     * @param ex the missing template
     * @return a 404 problem detail
     */
    @ExceptionHandler(ItemTemplateNotFoundException.class)
    public ProblemDetail handleItemTemplateNotFound(ItemTemplateNotFoundException ex) {
        LOG.warn("Item template not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage());
    }

    /**
     * @param ex the character inventory doesn't know
     * @return a 404 problem detail
     */
    @ExceptionHandler(CharacterNotFoundException.class)
    public ProblemDetail handleCharacterNotFound(CharacterNotFoundException ex) {
        LOG.warn("Character not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage());
    }

    /**
     * @param ex the code that is taken
     * @return a 409 problem detail
     */
    @ExceptionHandler(DuplicateCodeException.class)
    public ProblemDetail handleDuplicateCode(DuplicateCodeException ex) {
        LOG.warn("Duplicate code: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Duplicate Code", ex.getMessage());
    }

    /**
     * @param ex the missing set
     * @return a 404 problem detail
     */
    @ExceptionHandler(ItemSetNotFoundException.class)
    public ProblemDetail handleItemSetNotFound(ItemSetNotFoundException ex) {
        LOG.warn("Item set not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage());
    }

    /**
     * @param ex the set a template names
     * @return a 400 problem detail
     */
    @ExceptionHandler(UnknownItemSetException.class)
    public ProblemDetail handleUnknownItemSet(UnknownItemSetException ex) {
        LOG.warn("Unknown item set: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Unknown Item Set", ex.getMessage());
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
        return problem(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage());
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
