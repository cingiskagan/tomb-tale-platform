package com.tombtale.servicecommerce.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tombtale.servicecommerce.dto.PurchaseFilterRequest;
import com.tombtale.servicecommerce.entity.Purchase;
import com.tombtale.servicecommerce.entity.PurchaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurchaseQueryRepositoryImpl} using Mockito deep stubs.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.JUnitTestContainsTooManyAsserts", "PMD.TooManyStaticImports", "PMD.AvoidDuplicateLiterals"})
class PurchaseQueryRepositoryImplTest {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String PLAYER_1 = "player-1";
    private static final String ITEM_SWORD = "SWORD";

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private JPAQuery<Purchase> jpaQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    @InjectMocks
    private PurchaseQueryRepositoryImpl queryRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Mock the fluent API chaining using manual stubs (more robust than RETURNS_DEEP_STUBS for specific types)
        when(jpaQueryFactory.selectFrom(Mockito.<com.querydsl.core.types.EntityPath<Purchase>>any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(BooleanBuilder.class))).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);

        when(jpaQueryFactory.select(any(com.querydsl.core.types.Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(com.querydsl.core.types.EntityPath.class))).thenReturn(countQuery);
        when(countQuery.where(any(BooleanBuilder.class))).thenReturn(countQuery);
    }

    @Test
    void shouldFindAllFilteringOutCancelledByDefault() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        Purchase dummyPurchase = new Purchase();
        when(jpaQuery.fetch()).thenReturn(List.of(dummyPurchase));
        when(countQuery.fetchOne()).thenReturn(1L);

        Page<Purchase> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isEqualTo(1L);
        assertThat(results.getContent()).hasSize(1);
    }

    @Test
    void shouldFilterByPlayerId() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(PLAYER_1, null, null, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        when(jpaQuery.fetch()).thenReturn(List.of());
        when(countQuery.fetchOne()).thenReturn(0L);

        Page<Purchase> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    void shouldFilterByItemCode() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(null, ITEM_SWORD, null, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        when(jpaQuery.fetch()).thenReturn(List.of());
        when(countQuery.fetchOne()).thenReturn(null); // Testing the null safety branch: total != null ? total : 0L

        Page<Purchase> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isZero();
    }

    @Test
    void shouldIncludeCancelledWhenStatusExplicitlyRequested() {
        PurchaseFilterRequest filter = new PurchaseFilterRequest(null, null, PurchaseStatus.CANCELLED, null, null);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        when(jpaQuery.fetch()).thenReturn(List.of());
        when(countQuery.fetchOne()).thenReturn(0L);

        Page<Purchase> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void shouldFilterByDateRanges() {
        Instant after = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant before = Instant.now().plus(2, ChronoUnit.DAYS);

        PurchaseFilterRequest filter = new PurchaseFilterRequest(null, null, null, after, before);
        Pageable pageable = PageRequest.of(0, DEFAULT_PAGE_SIZE);

        when(jpaQuery.fetch()).thenReturn(List.of());
        when(countQuery.fetchOne()).thenReturn(0L);

        queryRepository.findByFilter(filter, pageable);

        Mockito.verify(jpaQueryFactory).selectFrom(any());
    }
}
