package org.mdental.authcore.config;

import lombok.RequiredArgsConstructor;
import org.mdental.security.tenant.TenantFilterInterceptor;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration.
 */
@Configuration
@ComponentScan("org.mdental.security.tenant")
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final TenantFilterInterceptor tenantFilterInterceptor;

    /**
     * Configure interceptors.
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFilterInterceptor);
    }
}