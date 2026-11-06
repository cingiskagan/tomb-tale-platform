package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.ItemIcon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Reads and writes the uploaded icons.
 */
public interface ItemIconRepository extends JpaRepository<ItemIcon, Long> {

    /**
     * @param hash the SHA-256 of the image, in lower-case hex
     * @return the icon, or empty when no upload had those bytes
     */
    Optional<ItemIcon> findByHash(String hash);

    /**
     * @param hash the SHA-256 of the image, in lower-case hex
     * @return whether an upload had those bytes
     */
    boolean existsByHash(String hash);
}
