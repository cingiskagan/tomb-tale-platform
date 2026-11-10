package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import com.tombtale.serviceinventory.domain.Stat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A group of templates whose bonus a character has while every one of them is equipped.
 */
@Entity
@Table(name = "item_sets")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ItemSet extends BaseEntity {

    private static final int CODE_MAX_LENGTH = 100;
    private static final int NAME_MAX_LENGTH = 100;

    /** A stable key such as {@code IRON_SET}, fixed at creation like a template's. */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = CODE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private String code;

    /** The name the portal and the game show. */
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    /** The stats the complete set adds. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bonus", nullable = false)
    private Map<Stat, Integer> bonus;

    /** The version an edit started from must still be this one. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
