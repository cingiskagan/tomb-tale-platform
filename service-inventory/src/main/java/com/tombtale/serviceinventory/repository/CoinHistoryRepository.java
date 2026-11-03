package com.tombtale.serviceinventory.repository;

import com.tombtale.serviceinventory.entity.CoinHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads and writes the history of every character's coins.
 */
public interface CoinHistoryRepository extends JpaRepository<CoinHistory, Long> {
}
