package com.tombtale.servicecommerce.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.servicecommerce.domain.PurchaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity representing an in-game purchase in the Tomb Tale economy.
 *
 * <p>Maps to the {@code purchases} table. Uses optimistic locking
 * ({@code @Version}), which stays on this entity rather than moving to
 * {@link BaseEntity}. Two things here earn it. {@code changeStatus} reads the
 * current status, asks the transition table whether a move is legal, and only
 * then writes — a decision taken from a value another transaction may already
 * have changed. And the row is a money ledger, so a lost update leaves a
 * stored total nobody approved.
 *
 * <p>Neither is true of the player entities: those assign whatever the request
 * sent, consulting no prior value, and last-write-wins is what that means.
 *
 * <p>Currency values ({@code unitPrice}, {@code totalPrice}) are stored
 * as {@link BigDecimal} to avoid floating-point rounding errors that
 * would corrupt the game economy ledger.
 *
 * <p>There is no {@code purchasedAt} field. It recorded the same instant as
 * the inherited {@code createdAt}, so it is gone rather than kept as a
 * duplicate. The API still calls that timestamp {@code purchasedAt}; the
 * mapper bridges the two names.
 */
@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Purchase extends BaseEntity {

    private static final int ITEM_CODE_MAX_LENGTH = 100;
    private static final int STATUS_MAX_LENGTH = 20;
    private static final int CURRENCY_PRECISION = 19;
    private static final int CURRENCY_SCALE = 4;

    /**
     * The {@code publicId} of the purchasing player, issued by service-player.
     *
     * <p>Not the Zitadel subject, which never leaves service-player, and not
     * that service's primary key, which never leaves its persistence layer.
     * There is no foreign key behind this column — commerce has no rights in
     * the player schema — so it is a reference this service cannot validate.
     * See ADR 0014.
     */
    @Column(name = "player_id", nullable = false)
    private UUID playerId;

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

    /** JPA optimistic-lock version — incremented on every update. */
    @Version
    @Column(name = "version")
    private Integer version;
}
