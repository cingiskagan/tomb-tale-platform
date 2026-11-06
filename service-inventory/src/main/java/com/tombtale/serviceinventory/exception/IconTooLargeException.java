package com.tombtale.serviceinventory.exception;

/**
 * An upload over {@code inventory.icons.max-size}. Answers 413.
 */
public class IconTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param maxBytes the limit the upload went over */
    public IconTooLargeException(long maxBytes) {
        super("An icon must be at most " + maxBytes + " bytes");
    }
}
