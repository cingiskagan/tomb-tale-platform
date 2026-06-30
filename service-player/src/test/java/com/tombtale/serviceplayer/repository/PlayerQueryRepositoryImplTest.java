package com.tombtale.serviceplayer.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tombtale.serviceplayer.dto.PlayerFilterRequest;
import com.tombtale.serviceplayer.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("PMD.TooManyStaticImports")
class PlayerQueryRepositoryImplTest {

    private static final int PAGE_SIZE = 10;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private JPAQuery<Player> jpaQuery;

    @Mock
    private JPAQuery<Long> countQuery;

    @InjectMocks
    private PlayerQueryRepositoryImpl queryRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(jpaQueryFactory.selectFrom(Mockito.<com.querydsl.core.types.EntityPath<Player>>any())).thenReturn(jpaQuery);
        when(jpaQuery.where(any(BooleanBuilder.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(com.querydsl.core.types.OrderSpecifier[].class))).thenReturn(jpaQuery);
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);

        when(jpaQueryFactory.select(any(com.querydsl.core.types.Expression.class))).thenReturn(countQuery);
        when(countQuery.from(any(com.querydsl.core.types.EntityPath.class))).thenReturn(countQuery);
        when(countQuery.where(any(BooleanBuilder.class))).thenReturn(countQuery);
    }

    @Test
    void shouldReturnFilteredPage() {
        PlayerFilterRequest filter = new PlayerFilterRequest("test");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Player dummyPlayer = new Player();

        when(jpaQuery.fetch()).thenReturn(List.of(dummyPlayer));
        when(countQuery.fetchOne()).thenReturn(1L);

        Page<Player> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isEqualTo(1L);
        assertThat(results.getContent()).hasSize(1);
        verify(jpaQuery).offset(pageable.getOffset());
        verify(jpaQuery).limit(pageable.getPageSize());
    }

    @Test
    void shouldThrowExceptionForInvalidSorting() {
        PlayerFilterRequest filter = new PlayerFilterRequest(null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "invalidField"));

        assertThatThrownBy(() -> queryRepository.findByFilter(filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid sort field: invalidField");
    }

    @Test
    void shouldReturnEmptyPageWhenTotalIsNull() {
        PlayerFilterRequest filter = new PlayerFilterRequest(null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        when(jpaQuery.fetch()).thenReturn(List.of());
        when(countQuery.fetchOne()).thenReturn(null);

        Page<Player> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isZero();
        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void shouldApplyValidAscendingSort() {
        PlayerFilterRequest filter = new PlayerFilterRequest(null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "displayName"));

        when(jpaQuery.fetch()).thenReturn(List.of(new Player()));
        when(countQuery.fetchOne()).thenReturn(1L);

        Page<Player> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void shouldApplyValidDescendingSort() {
        PlayerFilterRequest filter = new PlayerFilterRequest(null);
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(jpaQuery.fetch()).thenReturn(List.of(new Player()));
        when(countQuery.fetchOne()).thenReturn(1L);

        Page<Player> results = queryRepository.findByFilter(filter, pageable);

        assertThat(results.getTotalElements()).isEqualTo(1L);
    }
}
