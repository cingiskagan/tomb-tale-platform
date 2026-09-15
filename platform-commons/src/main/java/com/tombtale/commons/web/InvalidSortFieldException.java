package com.tombtale.commons.web;

/**
 * Thrown when a {@code ?sort=} parameter names a field that is not on the
 * repository's allow-list.
 *
 * <p>Its own type rather than {@link IllegalArgumentException}, so mapping it
 * to 400 does not also turn every unrelated argument bug into one.
 */
public class InvalidSortFieldException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param field the sort property the caller sent
     */
    public InvalidSortFieldException(String field) {
        super("Invalid sort field: " + field);
    }
}
