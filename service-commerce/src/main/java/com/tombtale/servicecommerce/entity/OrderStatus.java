package com.tombtale.servicecommerce.entity;

/**
 * Lifecycle status of a purchase order.
 * <p>
 * Transitions:
 * <ul>
 *     <li>{@code PENDING} → {@code COMPLETED} (payment accepted)</li>
 *     <li>{@code PENDING} → {@code CANCELLED} (player cancels before completion)</li>
 *     <li>{@code COMPLETED} → {@code REFUNDED} (admin-initiated reversal)</li>
 * </ul>
 */
public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    REFUNDED
}
