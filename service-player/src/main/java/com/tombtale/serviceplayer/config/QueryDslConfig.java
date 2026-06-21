package com.tombtale.serviceplayer.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link JPAQueryFactory} bean required by QueryDSL
 * repository implementations.
 */
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates a {@link JPAQueryFactory} backed by the application's
     * managed {@link EntityManager}.
     *
     * @return a JPAQueryFactory instance for type-safe JPA queries
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
