package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.CharacterInventory;
import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Reads and writes the stacks characters own.
 */
public interface ItemInstanceRepository extends JpaRepository<ItemInstance, Long> {

    /**
     * The character's stacks of this template that are not full yet, oldest first.
     *
     * @param character the character that owns the stacks
     * @param template  the template the stacks hold
     * @return the stacks that still have room
     */
    @Query("select i from ItemInstance i where i.characterInventory = :character and i.template = :template "
            + "and i.quantity < i.template.maxStackSize order by i.id")
    List<ItemInstance> findStacksWithRoom(
            @Param("character") CharacterInventory character, @Param("template") ItemTemplate template);
}
