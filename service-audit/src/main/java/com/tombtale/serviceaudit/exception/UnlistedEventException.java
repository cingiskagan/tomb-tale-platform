package com.tombtale.serviceaudit.exception;

/**
 * An event type or version with no allow-list. No retry can fix it, so the message goes to the DLQ at once
 * and waits there until someone reviews its fields.
 */
public class UnlistedEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Names the event that has no allow-list.
     *
     * @param eventType    the event's type
     * @param eventVersion the event's version
     */
    public UnlistedEventException(String eventType, int eventVersion) {
        super("No allow-list for " + eventType + " v" + eventVersion);
    }
}
