package com.tombtale.servicecommerce.config;

import com.tombtale.servicecommerce.security.CommerceAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Enables JPA auditing so the audit fields on entities extending
 * {@code BaseEntity} are filled: the two timestamps from
 * {@link #auditingDateTimeProvider()}, and the two actor columns from
 * {@link CommerceAuditorAware}.
 *
 * <p>No entity in this service extends {@code BaseEntity} yet — {@code Purchase}
 * does so in the commit that makes its identity a {@code publicId}. The wiring
 * lands first so that change is only about the entity.
 *
 * <p>The clock is a named bean rather than a call to {@code Instant.now()}
 * buried in the framework, so a test can replace it and decide what
 * {@code createdAt} a row gets. Without that seam the only way to write a row
 * with a past timestamp is to write around auditing.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "commerceAuditorAware",
        dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    AuditorAware<UUID> commerceAuditorAware() {
        return new CommerceAuditorAware();
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
