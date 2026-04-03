package com.tombtale.servicecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
@ActiveProfiles("test")
class ServiceCommerceApplicationTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @SuppressWarnings("PMD")
    void contextLoads() {
        // Intentionally empty: test passes if Spring context loads without throwing
    }

}
