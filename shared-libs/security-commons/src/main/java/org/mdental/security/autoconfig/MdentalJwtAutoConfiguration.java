package org.mdental.security.autoconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.security.exception.JwtExceptionHandler;
import org.mdental.security.filter.AuthTokenFilter;
import org.mdental.security.filter.ReactiveAuthFilter;
import org.mdental.security.jwt.JwtTokenProvider;
import org.mdental.security.password.PasswordService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

/**

 Auto-configuration for MDental JWT authentication
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JwtProps.class)
@RequiredArgsConstructor
@Schema(description = "Auto-configuration for JWT authentication")
public class MdentalJwtAutoConfiguration {

    private final JwtProps jwtProps;

    /**

     Provides a Clock bean for time-based operations
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
    /**

     Creates the JWT token provider
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenProvider jwtTokenProvider(Clock clock) {
        log.info("Configuring JWT token provider with issuer: {}", jwtProps.getIssuer());
        log.info("JWT signing mechanism: {}", jwtProps.isRsa() ? "RS256" : "HS256");
        return new JwtTokenProvider(jwtProps, clock);
    }
    /**

     Creates the password encoder (BCrypt with strength 12)
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    /**

     Creates the password service
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordService passwordService(PasswordEncoder passwordEncoder) {
        return new PasswordService(passwordEncoder);
    }
    /**

     Creates the JWT exception handler
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtExceptionHandler jwtExceptionHandler(ObjectMapper objectMapper) {
        return new JwtExceptionHandler(objectMapper);
    }
    /**

     Configuration for servlet applications (MVC)
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @RequiredArgsConstructor
    static class MvcJwtConfiguration {

        private final JwtTokenProvider jwtTokenProvider;
        private final ObjectMapper objectMapper;
        private final JwtExceptionHandler jwtExceptionHandler;
        private final Clock clock;

        /**

         Creates the JWT authentication filter for MVC applications
         */
        @Bean
        @ConditionalOnMissingBean
        public AuthTokenFilter authTokenFilter() {
            log.info("Configuring JWT filter for MVC applications");
            return new AuthTokenFilter(jwtTokenProvider, objectMapper, jwtExceptionHandler, clock);
        }
        /**

         Configures Spring Security to use our JWT filter
         */
        @Bean
        @ConditionalOnBean(HttpSecurity.class)
        public SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                AuthTokenFilter authTokenFilter) throws Exception {

            http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }
    }

    /**

     Configuration for reactive applications (WebFlux)
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = {"org.springframework.security.config.web.server.ServerHttpSecurity"})
    @RequiredArgsConstructor
    static class ReactiveJwtConfiguration {

        private final JwtTokenProvider jwtTokenProvider;
        private final ObjectMapper objectMapper;
        private final Clock clock;

        /**

         Creates the JWT authentication filter for reactive applications
         */
        @Bean
        @ConditionalOnMissingBean
        public ReactiveAuthFilter reactiveAuthFilter() {
            log.info("Configuring JWT filter for WebFlux applications");
            return new ReactiveAuthFilter(jwtTokenProvider, objectMapper, clock);
        }
    }
}