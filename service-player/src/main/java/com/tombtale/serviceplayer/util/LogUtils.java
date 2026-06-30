package com.tombtale.serviceplayer.util;

/**
 * Utility class for logging-related operations.
 */
public final class LogUtils {

    /**
     * Minimum length required to safely mask an ID while retaining privacy.
     */
    private static final int MIN_MASKABLE_ID_LENGTH = 8;

    /**
     * Number of visible characters left unmasked at the start and end of an ID.
     */
    private static final int VISIBLE_ID_CHARS = 4;

    private LogUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Masks an external identifier for safe logging (e.g., "1234abcd5678" ->
     * "1234***5678").
     * Keeps first 4 and last 4 characters visible for correlation.
     *
     * @param id the identifier to mask
     * @return the masked identifier, or "***" if too short
     */
    public static String maskId(String id) {
        if (id == null || id.length() <= MIN_MASKABLE_ID_LENGTH) {
            return "***"; // Too short to safely mask while retaining privacy
        }
        return id.substring(0, VISIBLE_ID_CHARS) + "***" +
                id.substring(id.length() - VISIBLE_ID_CHARS);
    }
}
