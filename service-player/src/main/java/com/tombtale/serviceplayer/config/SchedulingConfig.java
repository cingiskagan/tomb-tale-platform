package com.tombtale.serviceplayer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, which only
 * {@link com.tombtale.serviceplayer.service.ProvisioningReconciler} uses.
 *
 * <p>Its own class so the annotation is findable. Left on the application class
 * it would also start a scheduler in every {@code @SpringBootTest}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
