package org.mdental.security.autoconfig;

import lombok.RequiredArgsConstructor;
import org.mdental.security.jwt.DynamicIssuerJwtDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
}
