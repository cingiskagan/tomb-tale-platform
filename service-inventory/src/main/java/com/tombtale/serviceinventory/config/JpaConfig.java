package com.tombtale.serviceinventory.config;

import com.tombtale.commons.security.PlatformAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Fills the audit fields of {@code BaseEntity}: the author from {@link PlatformAuditorAware},
 * and the timestamps from a clock bean that a test can replace.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "inventoryAuditorAware",
        dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    AuditorAware<UUID> inventoryAuditorAware() {
        return new PlatformAuditorAware();
    }

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
