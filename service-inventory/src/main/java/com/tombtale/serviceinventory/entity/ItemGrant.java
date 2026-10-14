package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.serviceinventory.domain.ItemGrantOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Inventory's one item grant for a purchase, granted or rejected. A redelivered purchase finds it, so the items
 * are added once. Its {@code publicId} is the {@code eventId}, the same each time the item grant goes out.
 */
@Entity
@Table(name = "item_grants")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ItemGrant extends BaseEntity {

    private static final int ITEM_CODE_MAX_LENGTH = 100;
    private static final int OUTCOME_MAX_LENGTH = 20;
    private static final int REASON_MAX_LENGTH = 50;

    /** The purchase this item grant is for, as commerce named it in {@code purchase.created}. */
    @Column(name = "purchase_id", nullable = false, unique = true, updatable = false)
    private UUID purchaseId;

    /** The player the purchase names. */
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    /** The code the purchase names, kept as it arrived, also when the catalog does not know it. */
    @Column(name = "item_code", nullable = false, updatable = false, length = ITEM_CODE_MAX_LENGTH)
    private String itemCode;

    /** How many copies the purchase asked for. */
    @Column(name = "quantity", nullable = false, updatable = false)
    private Integer quantity;

    /** Whether inventory granted the items or refused them. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = OUTCOME_MAX_LENGTH)
    private ItemGrantOutcome outcome;

    /** Why inventory refused, such as {@code UNKNOWN_ITEM_CODE}. Null for a granted purchase. */
    @Column(name = "reason", updatable = false, length = REASON_MAX_LENGTH)
    private String reason;
}
