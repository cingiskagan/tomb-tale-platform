package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * An NPC that sells the items on its stand. It is retired, never deleted, so no new dealer takes over its code.
 */
@Entity
@Table(name = "dealers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Dealer extends BaseEntity {

    private static final int CODE_MAX_LENGTH = 100;
    private static final int NAME_MAX_LENGTH = 100;

    /** A stable key such as {@code TOWN_BLACKSMITH}, by which the Unity town finds the stand. */
    @Column(name = "code", nullable = false, unique = true, updatable = false, length = CODE_MAX_LENGTH)
    @Setter(AccessLevel.NONE)
    private String code;

    /** The name the portal and the game show. */
    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    /** A retired dealer's stand shows nothing. */
    @Column(name = "retired", nullable = false)
    private boolean retired;

    /** The version an edit started from must still be this one. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
