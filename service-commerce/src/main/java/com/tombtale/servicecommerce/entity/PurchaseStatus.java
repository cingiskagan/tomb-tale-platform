package com.tombtale.servicecommerce.entity;

/**
 * Represents the lifecycle states of an in-game purchase.
 *
 * <p>
 * Transitions:
 * <ul>
 * <li>{@code PENDING → COMPLETED} — payment confirmed and items delivered</li>
 * <li>{@code COMPLETED → REFUNDED} — purchase reversed, currency returned</li>
 * <li>{@code PENDING, COMPLETED, REFUNDED → CANCELLED} — soft-deleted, hidden from default queries</li>
 * </ul>
 */
public enum PurchaseStatus {

    /** Purchase created but not yet finalised. */
    PENDING,

    /** Payment confirmed and items delivered to the player. */
    COMPLETED,

    /** Purchase reversed; currency returned to the player. */
    REFUNDED,

    /** Soft-deleted — excluded from default list queries. */
    CANCELLED;

    /**
     * Tells whether this status may move directly to {@code target}.
     *
     * <p>
     * A self-transition is not a table move: {@code X.canTransitionTo(X)} is
     * always {@code false}. The service treats a same-status request as a
     * no-op before it consults this table.
     *
     * @param target the requested next status
     * @return true if the move is allowed
     */
    public boolean canTransitionTo(PurchaseStatus target) {
        return switch (this) {
            case PENDING -> target == COMPLETED || target == CANCELLED;
            case COMPLETED -> target == REFUNDED || target == CANCELLED;
            case REFUNDED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
