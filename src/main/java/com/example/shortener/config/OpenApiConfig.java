package com.example.shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shortener API")
                        .description("URL shortener service. Shorten long URLs and redirect to the original destination. " +
                                "Uses PostgreSQL for persistence, Redis for caching, and Bucket4j for rate limiting.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Shortener API"))
                        .license(new License()
                                .name("Unlicense")));
    }
}
