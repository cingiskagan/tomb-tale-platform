package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.serviceinventory.domain.CoinReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * One change to a character's coins, written in the same transaction as the change.
 */
@Entity
@Table(name = "coin_history")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CoinHistory extends BaseEntity {

    private static final int REASON_MAX_LENGTH = 30;

    /** The character whose coins changed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_inventory_id", nullable = false, updatable = false)
    private CharacterInventory characterInventory;

    /** The coins added, or taken when negative. */
    @Column(name = "amount", nullable = false, updatable = false)
    private Long amount;

    /** Why the coins changed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, updatable = false, length = REASON_MAX_LENGTH)
    private CoinReason reason;

    /** What made the change, such as a trade id. It is unique per character, so a change sent twice is found. */
    @Column(name = "reference", nullable = false, updatable = false)
    private UUID reference;
}
