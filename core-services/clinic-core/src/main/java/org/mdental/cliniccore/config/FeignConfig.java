package org.mdental.cliniccore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

@Configuration
public class FeignConfig {

    @Value("${INTERNAL_API_TOKEN:internal-api-token-placeholder}")
    private String internalApiToken;

    @Bean
    public RequestInterceptor serviceToServiceAuthInterceptor() {
        return requestTemplate ->
                requestTemplate.header("X-Internal-Service-Auth", internalApiToken);
    }
}