package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * A character as inventory knows it: its owner and its coins. Its {@code publicId} is the character's own,
 * from {@code player.created}, so a character has one id on the whole platform.
 */
@Entity
@Table(name = "character_inventories")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CharacterInventory extends BaseEntity {

    /** The owner's {@code publicId}. There is no foreign key, because players live in service-player (ADR 0014). */
    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    /** The character's coins. The schema refuses fewer than zero. */
    @Column(name = "coins", nullable = false)
    private Long coins;

    /** Of two coin changes made from the same read, the later commit fails. */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
