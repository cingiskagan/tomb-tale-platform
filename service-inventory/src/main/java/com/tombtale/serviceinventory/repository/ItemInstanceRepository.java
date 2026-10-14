package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.ItemInstance;
import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Reads and writes the stacks players own.
 */
public interface ItemInstanceRepository extends JpaRepository<ItemInstance, Long> {

    /**
     * The player's stacks of this template that are not full yet, oldest first.
     *
     * @param playerId the owner's {@code publicId}
     * @param template the template the stacks hold
     * @return the stacks that still have room
     */
    @Query("select i from ItemInstance i where i.playerId = :playerId and i.template = :template "
            + "and i.quantity < i.template.maxStackSize order by i.id")
    List<ItemInstance> findStacksWithRoom(@Param("playerId") UUID playerId, @Param("template") ItemTemplate template);
}
