package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/** Reads and writes the rows of the transactional outbox. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * The oldest pending rows, locked until commit. A lock timeout of -2 means SKIP LOCKED,
     * so a second publisher takes the next rows instead of sending these twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select e from OutboxEvent e where e.publishedAt is null order by e.id")
    List<OutboxEvent> findPendingForUpdate(Limit limit);

    /** Deletes the rows published before the cutoff and returns how many went. */
    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :cutoff")
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
