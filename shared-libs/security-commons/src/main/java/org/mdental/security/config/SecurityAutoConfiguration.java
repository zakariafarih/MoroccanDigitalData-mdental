package org.mdental.security.config;

import org.mdental.security.jwt.DynamicIssuerJwtDecoder;
import org.mdental.security.jwt.KeycloakJwtAuthenticationConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;

@Configuration
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder dynamicIssuerJwtDecoder(List<String> allowedIssuerPatterns) {
        return new DynamicIssuerJwtDecoder(allowedIssuerPatterns);
    }
}