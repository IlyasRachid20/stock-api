package com.ilyas.stockapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Title and description shown at the top of Swagger UI (/swagger-ui.html)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockApiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Stock API")
                .version("0.0.1")
                .description("Manage customers, products, sales and sale items. "
                        + "Selling an item takes it out of stock; deleting it puts it back."));
    }
}
