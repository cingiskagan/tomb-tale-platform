package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * One stack a player owns: copies of one template, up to its {@code maxStackSize}.
 */
@Entity
@Table(name = "item_instances")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ItemInstance extends BaseEntity {

    /** The owner's {@code publicId}. There is no foreign key, because players live in service-player (ADR 0014). */
    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    /** The catalog entry this stack holds copies of. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ItemTemplate template;

    /** How many copies the stack holds, from 1 to the template's {@code maxStackSize}. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** An item grant reads a stack before it fills it, so of two item grants on one stack, the later commit fails. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
