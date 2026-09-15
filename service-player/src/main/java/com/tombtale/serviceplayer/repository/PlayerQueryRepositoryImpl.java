package com.tombtale.serviceplayer.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tombtale.commons.web.InvalidSortFieldException;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.dto.PlayerResponse;
import com.tombtale.serviceplayer.entity.Player;
import com.tombtale.serviceplayer.entity.QPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * QueryDSL implementation of {@link PlayerQueryRepository}.
 *
 * <p>
 * Builds dynamic predicates from nullable filter fields so only
 * non-null criteria are applied.
 *
 * <p>
 * No {@code @Repository} annotation needed — Spring Data auto-discovers
 * this class by the {@code Impl} suffix naming convention.
 */
@RequiredArgsConstructor
public class PlayerQueryRepositoryImpl implements PlayerQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Builds a {@link BooleanBuilder} from every non-null field in
     * the filter, then executes a counted-pagination query.
     */
    @Override
    public Page<Player> findByFilter(
            PlayerFilterRequest filter,
            Pageable pageable) {
        QPlayer player = QPlayer.player;
        BooleanBuilder predicate = buildPredicate(filter, player);

        List<Player> results = jpaQueryFactory
                .selectFrom(player)
                .where(predicate)
                .orderBy(buildOrderSpecifiers(pageable.getSort(), player))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        fetchCharacters(results, player);

        Long total = jpaQueryFactory
                .select(player.count())
                .from(player)
                .where(predicate)
                .fetchOne();
        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(results, pageable, totalCount);
    }

    /**
     * Initialises the {@code characters} collection of an already-loaded page
     * of players with a single extra query.
     *
     * <p>The mapping is LAZY, and {@link PlayerResponse} carries characters, so
     * something has to load them. Left to itself Hibernate would load them one
     * player at a time — the N+1. This asks for all of them at once instead:
     * the fetch join runs in the same persistence context, so the collections
     * of the entities already in it come back initialised. Two queries for the
     * rows, whatever the page size.
     *
     * @param players the page of players already loaded, possibly empty
     * @param player  the Q-type path expression
     */
    private void fetchCharacters(List<Player> players, QPlayer player) {
        if (players.isEmpty()) {
            return;
        }

        jpaQueryFactory
                .selectFrom(player)
                .leftJoin(player.characters).fetchJoin()
                .where(player.in(players))
                .fetch();
    }

    /**
     * Translates nullable filter fields into QueryDSL predicates.
     *
     * @param filter the inbound filter (all fields nullable)
     * @param player the Q-type path expression
     * @return a composed predicate
     */
    private static BooleanBuilder buildPredicate(
            PlayerFilterRequest filter,
            QPlayer player) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.displayName() != null) {
            builder.and(player.displayName.containsIgnoreCase(
                    filter.displayName()));
        }

        return builder;
    }

    /**
     * Converts Spring Data {@link Sort} orders into QueryDSL
     * {@link OrderSpecifier} array.
     *
     * @param sort   the sort directives from the pageable
     * @param player the Q-type path expression
     * @return an array of order specifiers (empty if unsorted)
     * @throws InvalidSortFieldException if a sort property is not on the allow-list
     */
    private static OrderSpecifier<?>[] buildOrderSpecifiers(
            Sort sort,
            QPlayer player) {

        Set<String> allowedFields = Set.of(
                player.id.getMetadata().getName(),
                player.publicId.getMetadata().getName(),
                player.displayName.getMetadata().getName(),
                player.createdAt.getMetadata().getName());

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        PathBuilder<Player> entityPath = new PathBuilder<>(
                Player.class, player.getMetadata());

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!allowedFields.contains(property)) {
                throw new InvalidSortFieldException(property);
            }

            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(
                    direction,
                    entityPath.get(property, Comparable.class)));
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
