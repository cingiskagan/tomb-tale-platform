package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.serviceinventory.domain.EquipmentSlot;
import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.domain.Stat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * One stack a character owns: copies of one template and rarity, up to the template's {@code maxStackSize}.
 * It sits in an equipment slot or in a bag cell, never both.
 */
@Entity
@Table(name = "item_instances")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ItemInstance extends BaseEntity {

    private static final int RARITY_MAX_LENGTH = 10;
    private static final int EQUIPMENT_SLOT_MAX_LENGTH = 20;

    /** The character that owns the stack. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_inventory_id", nullable = false)
    private CharacterInventory characterInventory;

    /** The catalog entry this stack holds copies of. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private ItemTemplate template;

    /** Fixed when the item is made, like its bonus. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, updatable = false, length = RARITY_MAX_LENGTH)
    @Builder.Default
    private Rarity rarity = Rarity.GRAY;

    /** The stats the item rolled on top of its template's. A copy with a bonus never stacks. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bonus", nullable = false, updatable = false)
    @Builder.Default
    private Map<Stat, Integer> bonus = Map.of();

    /** How many copies the stack holds, from 1 to the template's {@code maxStackSize}. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** The slot the item is equipped in, or null while it is in the bag. */
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_slot", length = EQUIPMENT_SLOT_MAX_LENGTH)
    private EquipmentSlot equipmentSlot;

    /** The item's cell in the bag, or null while it is equipped. */
    @Column(name = "bag_position")
    private Integer bagPosition;

    /** An item grant reads a stack before it fills it, so of two item grants on one stack, the later commit fails. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
