package com.ilyas.stockapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Title and description shown at the top of Swagger UI (/swagger-ui.html), and its
// "Authorize" button: paste the accessToken from POST /api/auth/login there
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock API")
                        .version("0.0.1")
                        .description("Manage customers, products, sales and sale items. "
                                + "Selling an item takes it out of stock; deleting it puts it back. "
                                + "Log in with POST /api/auth/login, then click Authorize and paste the accessToken."))
                .components(new Components().addSecuritySchemes("bearer-jwt", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
