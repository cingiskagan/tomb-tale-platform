package com.tombtale.servicecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an in-game purchase in the Tomb Tale economy.
 *
 * <p>Maps to the {@code purchases} table. Uses optimistic locking
 * ({@code @Version}) to prevent lost-update anomalies on concurrent
 * modifications.
 *
 * <p>Currency values ({@code unitPrice}, {@code totalPrice}) are stored
 * as {@link BigDecimal} to avoid floating-point rounding errors that
 * would corrupt the game economy ledger.
 */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    private static final int ITEM_CODE_MAX_LENGTH = 100;
    private static final int STATUS_MAX_LENGTH = 20;
    private static final int CURRENCY_PRECISION = 19;
    private static final int CURRENCY_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Zitadel user subject — identifies the purchasing player. */
    @Column(name = "player_id", nullable = false)
    private String playerId;

    /** Catalogue item identifier (e.g. {@code SWORD_IRON}). */
    @Column(name = "item_code", nullable = false, length = ITEM_CODE_MAX_LENGTH)
    private String itemCode;

    /** Number of items purchased (must be ≥ 1). */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Price per single item in the smallest currency unit. */
    @Column(name = "unit_price", nullable = false, precision = CURRENCY_PRECISION, scale = CURRENCY_SCALE)
    private BigDecimal unitPrice;

    /** {@code quantity × unitPrice} — stored for ledger integrity. */
    @Column(name = "total_price", nullable = false, precision = CURRENCY_PRECISION, scale = CURRENCY_SCALE)
    private BigDecimal totalPrice;

    /** Current lifecycle state of this purchase. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = STATUS_MAX_LENGTH)
    private PurchaseStatus status;

    /** Timestamp when the purchase was originally created (UTC). */
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private Instant purchasedAt;

    /** JPA optimistic-lock version — incremented on every update. */
    @Version
    @Column(name = "version")
    private Integer version;

    /**
     * Sets the {@code purchasedAt} timestamp to the current instant
     * if it has not already been set before the entity is first persisted.
     */
    @PrePersist
    protected void onPrePersist() {
        if (purchasedAt == null) {
            purchasedAt = Instant.now();
        }
    }
}
