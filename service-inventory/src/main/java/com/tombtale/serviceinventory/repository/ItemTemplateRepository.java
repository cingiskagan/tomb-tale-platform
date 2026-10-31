package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.ItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Reads the item catalog.
 */
public interface ItemTemplateRepository extends JpaRepository<ItemTemplate, Long> {

    /**
     * Finds the template a purchase names. The match is exact, so {@code sword_iron} is unknown.
     *
     * @param code the template's code, such as {@code SWORD_IRON}
     * @return the template, or empty when the catalog does not know the code
     */
    Optional<ItemTemplate> findByCode(String code);
}
