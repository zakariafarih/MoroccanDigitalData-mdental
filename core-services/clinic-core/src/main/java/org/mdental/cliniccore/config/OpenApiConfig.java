package org.mdental.cliniccore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic Core API")
                        .version("1.0.0")
                        .description("REST APIs for managing dental clinic metadata")
                        .contact(new Contact()
                                .name("MDental Support")
                                .email("support@mdental.org")
                                .url("https://mdental.org"))
                        .license(new License()
                                .name("Private License")
                                .url("https://mdental.org/license")));
    }

    /**
     * This bean tells the Swagger-UI where to post your spec for validation
     * and enables the Try-It-Out button.
     */
    @Autowired
    public void customizeSwaggerUi(SwaggerUiConfigParameters config) {
        // point at the online Swagger validator
        config.setValidatorUrl("https://validator.swagger.io/validator");
        // turn on Try-It-Out by default
        config.setTryItOutEnabled(true);
    }
}