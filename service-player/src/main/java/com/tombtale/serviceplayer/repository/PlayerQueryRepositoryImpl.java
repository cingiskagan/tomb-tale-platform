package com.tombtale.serviceplayer.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.model.Player;
import com.tombtale.serviceplayer.model.QPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

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

        Long total = jpaQueryFactory
                .select(player.count())
                .from(player)
                .where(predicate)
                .fetchOne();
        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(results, pageable, totalCount);
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
        if (filter.minLevel() != null) {
            builder.and(player.level.goe(filter.minLevel()));
        }
        if (filter.maxLevel() != null) {
            builder.and(player.level.loe(filter.maxLevel()));
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
     */
    private static OrderSpecifier<?>[] buildOrderSpecifiers(
            Sort sort,
            QPlayer player) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        PathBuilder<Player> entityPath = new PathBuilder<>(
                Player.class, player.getMetadata());

        for (Sort.Order order : sort) {
            Order direction = order.isAscending()
                    ? Order.ASC
                    : Order.DESC;
            orders.add(new OrderSpecifier<>(
                    direction,
                    entityPath.get(order.getProperty(), Comparable.class)));
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
