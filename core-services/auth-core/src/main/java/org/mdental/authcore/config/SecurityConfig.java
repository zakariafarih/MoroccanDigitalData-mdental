package org.mdental.authcore.config;

import lombok.RequiredArgsConstructor;
import org.mdental.authcore.infrastructure.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final MdcFilter mdcFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final PasswordResetRateLimitFilter passwordResetRateLimitFilter;

    /**
     * Configure security filter chain.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain security(HttpSecurity http) throws Exception {
        // Create CSRF token repository that uses cookies and supports Angular, React, etc.
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // Use __Secure- prefix for CSRF cookie in HTTPS environments
        csrfTokenRepository.setCookieName("__Secure-XSRF-TOKEN");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");

        // This helps with SPA frameworks that use different token extraction methods
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                "/internal/**",
                                "/.well-known/jwks.json"
                        )
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/docs/**", "/swagger-ui/**", "/.well-known/jwks.json").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/internal/**").hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated())
                // Password reset filter should come before login filter
                // Both rate-limit filters should run before the username/password auth filter
                .addFilterBefore(passwordResetRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter,       UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityHeadersFilter,      UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,     UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mdcFilter,                   SecurityHeadersFilter.class)
                .build();
    }

    /**
     * Configure CORS settings.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}