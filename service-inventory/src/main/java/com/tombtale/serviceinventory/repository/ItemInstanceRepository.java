package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.domain.Rarity;
import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and writes the stacks characters own.
 */
public interface ItemInstanceRepository extends JpaRepository<ItemInstance, Long> {

    /**
     * The character's stacks of this template and rarity that have no bonus and are not full yet, oldest first.
     *
     * @param character the character that owns the stacks
     * @param template  the template the stacks hold
     * @param rarity    the rarity the stacks hold
     * @return the stacks that still have room
     */
    @Query("select i from ItemInstance i where i.characterInventory = :character and i.template = :template "
            + "and i.rarity = :rarity and cast(i.bonus as String) = '{}' "
            + "and i.quantity < i.template.maxStackSize order by i.id")
    List<ItemInstance> findStacksWithRoom(
            @Param("character") CharacterInventory character,
            @Param("template") ItemTemplate template,
            @Param("rarity") Rarity rarity);

    /**
     * @param character the character that owns the items
     * @return the bag cells its items take, in no order
     */
    @Query("select i.bagPosition from ItemInstance i where i.characterInventory = :character "
            + "and i.bagPosition is not null")
    List<Integer> findBagPositions(@Param("character") CharacterInventory character);

    /**
     * Every item of the character, with its template and set in the same query.
     *
     * @param character the character that owns the items
     * @return its items, equipped and in the bag
     */
    @EntityGraph(attributePaths = {"template", "template.itemSet"})
    List<ItemInstance> findByCharacterInventory(CharacterInventory character);

    /**
     * @param publicId  the item's id in the API
     * @param character the character that must own it
     * @return the item, or empty when the character owns none with the id
     */
    Optional<ItemInstance> findByPublicIdAndCharacterInventory(UUID publicId, CharacterInventory character);
}
