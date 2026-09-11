package com.tombtale.servicecommerce.config;

import com.tombtale.servicecommerce.security.CommerceAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

/**
 * Enables JPA auditing so the audit fields on entities extending
 * {@code BaseEntity} are filled: the two timestamps from the clock, and the
 * two actor columns from {@link CommerceAuditorAware}.
 *
 * <p>No entity in this service extends {@code BaseEntity} yet — {@code Purchase}
 * does so in the commit that makes its identity a {@code publicId}. The wiring
 * lands first so that change is only about the entity.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "commerceAuditorAware")
public class JpaConfig {

    @Bean
    AuditorAware<UUID> commerceAuditorAware() {
        return new CommerceAuditorAware();
    }
}
