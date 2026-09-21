package com.tombtale.serviceplayer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled}, which only
 * {@link com.tombtale.serviceplayer.service.ProvisioningReconciler} uses.
 *
 * <p>On unless {@code app.scheduling.enabled} says otherwise, so no environment
 * loses the sweep by omission. The test profile turns it off: a
 * {@code @SpringBootTest} loads this class, and a sweep firing mid-test would
 * call out to a Zitadel that is not there.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
