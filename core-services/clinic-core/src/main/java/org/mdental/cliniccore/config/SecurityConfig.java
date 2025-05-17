package org.mdental.cliniccore.config;

import org.mdental.security.jwt.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** issuer patterns injected once and reused for both the bean and the filter-chain */
    @Value("#{'${mdental.auth.allowed-issuer-patterns}'.split(',')}")
    private List<String> allowedIssuerPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")) // Only disable CSRF for API endpoints
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints
                        .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**").permitAll()
                        // Dev-only endpoints (restricted by profile via annotation)
                        .requestMatchers("/internal/dev/**").permitAll()
                        // Require authentication for API endpoints
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter())
                        ));

        return http.build();
    }

    /**
     * Only for development and test profiles - mock JWT decoder
     * This avoids the need to connect to a real Keycloak server during development and tests
     */
    @Bean
    @Profile({"dev", "local", "test"})
    public JwtDecoder jwtDecoder() {
        // Using a dummy key for test purposes
        String jwkSetUri = "https://localhost:8080/.well-known/jwks.json";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}