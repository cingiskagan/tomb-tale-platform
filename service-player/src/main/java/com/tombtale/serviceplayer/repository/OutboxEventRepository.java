package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Reads and writes the rows of the transactional outbox. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
}
