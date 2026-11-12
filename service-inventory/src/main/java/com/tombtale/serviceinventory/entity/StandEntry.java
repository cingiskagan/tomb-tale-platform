package com.tombtale.serviceinventory.entity;

import com.tombtale.commons.entity.BaseEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * One item a dealer sells, which never sells out. An admin adds and removes entries, and never edits one.
 */
@Entity
@Table(name = "stand_entries")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StandEntry extends BaseEntity {

    private static final int RARITY_MAX_LENGTH = 10;

    /** The dealer whose stand shows it. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealer_id", nullable = false, updatable = false)
    private Dealer dealer;

    /** What a buy gives a copy of. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false, updatable = false)
    private ItemTemplate template;

    /** The rarity every copy has, which sets the price. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false, updatable = false, length = RARITY_MAX_LENGTH)
    private Rarity rarity;

    /** The bonus every copy of a custom entry carries, or null when each copy rolls its own. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bonus", updatable = false)
    private Map<Stat, Integer> bonus;

    /**
     * @return what a copy costs: its template's price times its rarity's multiplier
     */
    public long price() {
        return template.getPrice() * rarity.priceMultiplier();
    }
}
