package com.codeexplainer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeExplainerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Code Explainer API")
                        .description("REST API for analyzing Java JAR files and generating documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Code Explainer Team")
                                .email("support@codeexplainer.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
