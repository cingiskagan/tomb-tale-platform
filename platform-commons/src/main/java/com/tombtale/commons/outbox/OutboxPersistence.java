package com.tombtale.commons.outbox;

import com.tombtale.commons.entity.OutboxEvent;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/**
 * Adds the outbox's entity and repository to the packages Spring Boot scans, beside the service's own.
 * A service imports it on its main class, so that test slices find them too.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage(basePackageClasses = { OutboxEvent.class, OutboxEventRepository.class })
public class OutboxPersistence {
}
