package com.tombtale.servicecommerce.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import com.tombtale.servicecommerce.entity.QPurchase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryDSL implementation of {@link PurchaseQueryRepository}.
 *
 * <p>Builds dynamic predicates from nullable filter fields so only
 * non-null criteria are applied. Soft-deleted rows ({@code CANCELLED})
 * are excluded unless explicitly requested via the filter.
 *
 * <p>No {@code @Repository} annotation needed — Spring Data auto-discovers
 * this class by the {@code Impl} suffix naming convention.
 */
@RequiredArgsConstructor
public class PurchaseQueryRepositoryImpl implements PurchaseQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * {@inheritDoc}
     *
     * <p>Builds a {@link BooleanBuilder} from every non-null field in
     * the filter, then executes a counted-pagination query.
     */
    @Override
    public Page<Purchase> findByFilter(PurchaseFilterRequest filter, Pageable pageable) {
        QPurchase purchase = QPurchase.purchase;
        BooleanBuilder predicate = buildPredicate(filter, purchase);

        List<Purchase> results = jpaQueryFactory
                .selectFrom(purchase)
                .where(predicate)
                .orderBy(buildOrderSpecifiers(pageable.getSort(), purchase))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(purchase.count())
                .from(purchase)
                .where(predicate)
                .fetchOne();
        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(results, pageable, totalCount);
    }

    /**
     * Translates nullable filter fields into QueryDSL predicates.
     *
     * <p>When the filter does not explicitly specify a status, the query
     * automatically excludes {@code CANCELLED} purchases (soft-delete).
     *
     * @param filter   the inbound filter (all fields nullable)
     * @param purchase the Q-type path expression
     * @return a composed predicate
     */
    private static BooleanBuilder buildPredicate(PurchaseFilterRequest filter, QPurchase purchase) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.playerId() != null) {
            builder.and(purchase.playerId.eq(filter.playerId()));
        }
        if (filter.itemCode() != null) {
            builder.and(purchase.itemCode.eq(filter.itemCode()));
        }
        if (filter.status() != null) {
            builder.and(purchase.status.eq(filter.status()));
        } else {
            builder.and(purchase.status.ne(PurchaseStatus.CANCELLED));
        }
        if (filter.purchasedAfter() != null) {
            builder.and(purchase.purchasedAt.goe(filter.purchasedAfter()));
        }
        if (filter.purchasedBefore() != null) {
            builder.and(purchase.purchasedAt.loe(filter.purchasedBefore()));
        }

        return builder;
    }

    /**
     * Converts Spring Data {@link Sort} orders into QueryDSL {@link OrderSpecifier} array.
     *
     * @param sort     the sort directives from the pageable
     * @param purchase the Q-type path expression
     * @return an array of order specifiers (empty if unsorted)
     */
    private static OrderSpecifier<?>[] buildOrderSpecifiers(Sort sort, QPurchase purchase) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        PathBuilder<Purchase> entityPath = new PathBuilder<>(Purchase.class, purchase.getMetadata());

        for (Sort.Order order : sort) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            orders.add(new OrderSpecifier<>(direction, entityPath.get(order.getProperty(), Comparable.class)));
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
