package com.tombtale.serviceplayer.config;

import com.tombtale.serviceplayer.repository.PlayerRepository;
import com.tombtale.serviceplayer.security.PlayerAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

/**
 * Enables JPA auditing, which fills the four audit fields on every entity
 * extending {@code BaseEntity}: {@code @CreatedDate} and
 * {@code @LastModifiedDate} from the clock, {@code @CreatedBy} and
 * {@code @LastModifiedBy} from {@link PlayerAuditorAware}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "playerAuditorAware")
public class JpaConfig {

    @Bean
    AuditorAware<UUID> playerAuditorAware(PlayerRepository playerRepository) {
        return new PlayerAuditorAware(playerRepository);
    }
}
