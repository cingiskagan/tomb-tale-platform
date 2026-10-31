package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One entry in the item catalog. The portal writes these rows, and every other service names a template by its
 * {@code code}, never by {@code publicId}, because a code is the same in every environment.
 */
@Entity
@Table(name = "item_templates")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ItemTemplate extends BaseEntity {

    private static final int CODE_MAX_LENGTH = 100;

    /** A stable key such as {@code SWORD_IRON}. A new code is a new template, so it never changes. */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = CODE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private String code;

    /** How many copies one stack holds. 1 means each copy is its own row. */
    @Column(name = "max_stack_size", nullable = false)
    private Integer maxStackSize;
}
