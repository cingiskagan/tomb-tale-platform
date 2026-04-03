package com.tombtale.servicecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration for the commerce service.
 *
 * <p>Registers a JWT bearer security scheme so authenticated endpoints
 * can be tested directly from the Swagger UI once Zitadel integration
 * is re-enabled on the frontend.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-jwt";
    private static final String API_TITLE = "Tomb Tale — Commerce Service API";
    private static final String API_VERSION = "v1";
    private static final String API_DESCRIPTION =
            "CRUD operations for in-game purchases with ACID transactional guarantees.";

    /**
     * Builds the OpenAPI specification with service metadata and a JWT
     * security scheme definition.
     *
     * @return the configured OpenAPI instance
     */
    @Bean
    public OpenAPI commerceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .version(API_VERSION)
                        .description(API_DESCRIPTION))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste a valid Zitadel-issued JWT here")));
    }
}
