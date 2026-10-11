package com.tombtale.serviceinventory;

import com.tombtale.serviceinventory.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The real context on a real Postgres: Flyway migrates, and Hibernate's {@code validate} checks every entity
 * against the schema it built.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceInventoryApplicationTests extends PostgresTestBase {

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }
}
