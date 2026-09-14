package com.tombtale.commons.web;

/**
 * Thrown when a {@code ?sort=} parameter names a field that is not on the
 * repository's allow-list.
 *
 * <p>
 * Both query repositories validate sort properties before handing them to
 * QueryDSL, so a caller cannot steer the generated HQL through the order-by
 * clause. Until now both threw {@link IllegalArgumentException}, which nothing
 * handled: a typo in a query string came back as a 500. A dedicated type lets
 * {@link PlatformExceptionHandler} map it to 400 without also swallowing every
 * other {@code IllegalArgumentException} a service might throw.
 */
public class InvalidSortFieldException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception naming the rejected sort field.
     *
     * @param field the sort property the caller sent
     */
    public InvalidSortFieldException(String field) {
        super("Invalid sort field: " + field);
    }
}
