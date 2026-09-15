package com.tombtale.commons.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The error contract both services answer with: RFC 9457 problem details.
 * See ADR 0015.
 *
 * <p>Each service subclasses this with a {@code @RestControllerAdvice} and adds
 * only the exceptions it alone can throw. Cross-cutting ones live here.
 *
 * <p>Access denial is deliberately absent. Spring Security's filter chain
 * translates it, and that is what tells a missing token (401) from an
 * insufficient role (403).
 */
public abstract class PlatformExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformExceptionHandler.class);

    private static final String OPTIMISTIC_LOCK_DETAIL =
            "The resource was modified by another transaction. Please retry.";

    /** Extension member carrying per-field validation messages. */
    private static final String ERRORS_PROPERTY = "errors";

    /**
     * Maps a rejected {@code ?sort=} field to 400.
     *
     * @param ex the rejected sort field
     * @return a 400 problem detail
     */
    @ExceptionHandler(InvalidSortFieldException.class)
    public ProblemDetail handleInvalidSortField(InvalidSortFieldException ex) {
        LOG.warn("Rejected sort parameter: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid Sort Field", ex.getMessage());
    }

    /**
     * Maps a lost optimistic-locking race to 409.
     *
     * <p>The detail is fixed text: the exception message names the entity and
     * its primary key, which do not belong in a response.
     *
     * @param ex the locking failure
     * @return a 409 problem detail
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockConflict(ObjectOptimisticLockingFailureException ex) {
        LOG.warn("Optimistic lock conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", OPTIMISTIC_LOCK_DETAIL);
    }

    /**
     * Adds per-field messages to the framework's validation problem.
     *
     * <p>The inherited {@code detail} is the constant "Invalid request
     * content.", so the field errors go in an {@code errors} member instead.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Objects.requireNonNullElse(fieldError.getDefaultMessage(), "is invalid"),
                        (first, second) -> first + "; " + second,
                        LinkedHashMap::new));
        LOG.warn("Validation failed: {}", fieldErrors);

        ProblemDetail body = ex.getBody();
        body.setTitle("Validation Error");
        body.setProperty(ERRORS_PROPERTY, fieldErrors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Builds a titled problem detail, for subclasses to reuse.
     *
     * @param status the HTTP status
     * @param title  a short, stable label for this class of error
     * @param detail an explanation safe to show a client
     * @return the populated problem detail
     */
    protected static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
