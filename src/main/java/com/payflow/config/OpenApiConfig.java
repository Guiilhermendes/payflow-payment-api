package com.payflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI payflowApi() {
        return new OpenAPI().info(new Info().title("PayFlow API")
                .description("Demonstration payment processing API. POST /api/payments requires Idempotency-Key.")
                .version("1.0.0"));
    }
}
