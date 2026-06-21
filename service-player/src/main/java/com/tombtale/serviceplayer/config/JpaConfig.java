package com.tombtale.serviceplayer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so that @CreatedDate and @LastModifiedDate
 * annotations on entities are automatically populated.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
