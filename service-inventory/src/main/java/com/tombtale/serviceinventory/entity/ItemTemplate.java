package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.serviceinventory.domain.ItemType;
import com.tombtale.serviceinventory.domain.Stat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

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
    private static final int NAME_MAX_LENGTH = 100;
    private static final int ITEM_TYPE_MAX_LENGTH = 30;
    private static final int ASSET_KEY_MAX_LENGTH = 200;

    /** A stable key such as {@code SWORD_IRON}. A new code is a new template, so it never changes. */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = CODE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private String code;

    /** The name the portal and the game show. */
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    /** Fixed at creation, because an equipped copy sits in a slot its type chose. */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, updatable = false, length = ITEM_TYPE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private ItemType itemType;

    /** How many copies one stack holds. 1 means each copy is its own row. */
    @Column(name = "max_stack_size", nullable = false)
    private Integer maxStackSize;

    /** What a gray copy costs at a dealer, in coins. */
    @Column(name = "price", nullable = false)
    private Long price;

    /** The stats every copy has before its bonus. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "base_stats", nullable = false)
    private Map<Stat, Integer> baseStats;

    /** The Addressables address of the template's asset in the Unity client. */
    @Column(name = "asset_key", nullable = false, length = ASSET_KEY_MAX_LENGTH)
    private String assetKey;

    /** A retired template is never sold or granted again, and the copies players own keep it. */
    @Column(name = "retired", nullable = false)
    private boolean retired;

    /** The version an edit started from must still be this one. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
