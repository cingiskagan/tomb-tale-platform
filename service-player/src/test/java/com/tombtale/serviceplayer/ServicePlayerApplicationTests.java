package com.tombtale.serviceplayer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.tombtale.serviceplayer.support.PostgresTestBase;

@SpringBootTest
@ActiveProfiles("test")
class ServicePlayerApplicationTests extends PostgresTestBase {

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }

}
