package com.tombtale.serviceplayer.repository;

import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.model.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom query interface for dynamic, type-safe player filtering via QueryDSL.
 *
 * <p>
 * Implementations use {@code JPAQueryFactory} to build predicates
 * from {@link PlayerFilterRequest} fields at runtime.
 */
@FunctionalInterface
public interface PlayerQueryRepository {

    /**
     * Returns a paginated list of players matching the given filter criteria.
     *
     * @param filter   optional filter fields (all nullable — null = no filter)
     * @param pageable pagination and sorting parameters
     * @return a page of matching players
     */
    Page<Player> findByFilter(PlayerFilterRequest filter, Pageable pageable);
}
