package com.tombtale.serviceinventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI for inventory, with the bearer scheme service-commerce uses. Paste a Keycloak token to try an endpoint.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    /** @return the API's title and its security scheme */
    @Bean
    public OpenAPI inventoryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tomb Tale — Inventory Service API")
                        .version("v1")
                        .description("The item catalog and the items and coins characters hold."))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste a valid Keycloak-issued JWT here")));
    }
}
