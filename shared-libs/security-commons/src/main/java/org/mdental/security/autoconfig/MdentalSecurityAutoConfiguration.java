package org.mdental.security.autoconfig;

import lombok.RequiredArgsConstructor;
import org.mdental.security.jwt.DynamicIssuerJwtDecoder;
import org.mdental.security.jwt.KeycloakJwtAuthenticationConverter;
import org.mdental.security.tenant.ReactiveTenantFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
@EnableConfigurationProperties(MdentalSecurityProperties.class)
@RequiredArgsConstructor
public class MdentalSecurityAutoConfiguration {

    private final MdentalSecurityProperties props;

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder mdentalJwtDecoder() {
        return new DynamicIssuerJwtDecoder(props.getAllowedIssuerPatterns());
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveTenantFilter reactiveTenantFilter() {
        return new ReactiveTenantFilter();
    }
}