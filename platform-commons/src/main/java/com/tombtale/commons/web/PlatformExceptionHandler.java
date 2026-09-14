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
 * The error contract both services answer with: RFC 9457 {@code ProblemDetail},
 * served as {@code application/problem+json}.
 *
 * <p>
 * Before this class, a client needed two error parsers for one platform.
 * Commerce returned a hand-rolled {@code ErrorResponse} record; player threw
 * {@code ResponseStatusException} and let Spring's default error page answer,
 * which drops the message. Extending
 * {@link ResponseEntityExceptionHandler} fixes the second case for free: it
 * already handles every framework exception, and
 * {@code ResponseStatusException} is an {@code ErrorResponseException}, so its
 * reason reaches the client as {@code detail}.
 *
 * <p>
 * Registering any {@code ResponseEntityExceptionHandler} bean also makes Boot
 * back off its own {@code ProblemDetailsExceptionHandler}, so
 * {@code spring.mvc.problemdetails.enabled} is not needed and is deliberately
 * not set.
 *
 * <p>
 * <b>Subclassing:</b> each service declares a {@code @RestControllerAdvice}
 * extending this class and adds {@code @ExceptionHandler} methods for its own
 * domain exceptions. Only cross-cutting failures belong here — anything a
 * single service can throw stays in that service.
 *
 * <p>
 * Access denial is not handled here on purpose. {@code AccessDeniedException}
 * is translated by Spring Security's filter chain, which is what tells a
 * missing token (401) from an insufficient role (403); catching it in an
 * advice would flatten both to 403.
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
     * <p>
     * The message names the field the caller sent, never the entity field
     * behind it — the allow-list is the contract, and echoing the internal
     * name would leak the mapping.
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
     * <p>
     * The client's own detail is fixed text: the exception message names the
     * entity class and its primary key, neither of which belongs in a response.
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
     * <p>
     * The inherited handler produces a correct 400 whose {@code detail} is the
     * constant "Invalid request content." — true but useless to a form. The
     * field errors go in an {@code errors} extension member instead of being
     * joined into one prose string, so a client can point at the field that
     * failed rather than parse a sentence.
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
     * Builds a problem detail with a title, for subclasses to reuse.
     *
     * @param status the HTTP status
     * @param title  a short, stable label for this class of error
     * @param detail a human-readable explanation, safe to show a client
     * @return the populated problem detail
     */
    protected static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
