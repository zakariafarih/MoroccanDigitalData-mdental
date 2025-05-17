package org.mdental.cliniccore.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@Configuration
@EnableFeignClients(basePackages = "org.mdental.cliniccore.client")
public class FeignConfig {

    @Value("${INTERNAL_API_TOKEN:internal-api-token-placeholder}")
    private String internalApiToken;

    @Bean
    public RequestInterceptor bearerTokenInterceptor() {
        return requestTemplate ->
                requestTemplate.header("Authorization", "Bearer " + internalApiToken);
    }
}