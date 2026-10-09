package com.tombtale.serviceplayer.config;

import com.tombtale.commons.security.PlatformAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

/**
 * Enables JPA auditing, which fills the four audit fields on every entity
 * extending {@code BaseEntity}: {@code @CreatedDate} and
 * {@code @LastModifiedDate} from the clock, {@code @CreatedBy} and
 * {@code @LastModifiedBy} from {@link PlatformAuditorAware}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "playerAuditorAware")
public class JpaConfig {

    @Bean
    AuditorAware<UUID> playerAuditorAware() {
        return new PlatformAuditorAware();
    }
}
