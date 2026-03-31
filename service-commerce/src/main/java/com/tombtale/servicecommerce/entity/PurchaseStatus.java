package com.tombtale.servicecommerce.entity;

/**
 * Represents the lifecycle states of an in-game purchase.
 *
 * <p>Transitions:
 * <ul>
 *   <li>{@code PENDING → COMPLETED} — payment confirmed and items delivered</li>
 *   <li>{@code COMPLETED → REFUNDED} — purchase reversed, currency returned</li>
 *   <li>{@code * → CANCELLED} — soft-deleted, hidden from default queries</li>
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
    CANCELLED
}
