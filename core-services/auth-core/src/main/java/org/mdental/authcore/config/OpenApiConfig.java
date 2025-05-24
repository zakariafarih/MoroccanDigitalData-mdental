package org.mdental.authcore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI documentation.
     *
     * @return the OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Core API")
                        .version("1.0.0")
                        .description("Authentication and authorization APIs for MDental platform")
                        .contact(new Contact()
                                .name("MDental Support")
                                .email("support@mdental.org")
                                .url("https://mdental.org"))
                        .license(new License()
                                .name("Private License")
                                .url("https://mdental.org/license")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Authorization header using the Bearer scheme. Example: 'Bearer {token}'")));
    }
}