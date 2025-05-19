package org.mdental.authcore.config.security;

import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.Role;
import org.mdental.security.jwt.KeycloakJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthCoreSecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("#{'${mdental.auth.allowed-issuer-patterns}'.split(',')}")
    private List<String> allowedIssuerPatterns;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/api-docs/**", "/swagger-ui/**", "/docs/**").permitAll()
                        .requestMatchers("/oauth/token", "/oauth/introspect", "/oauth/jwks").hasAuthority("SCOPE_gateway")
                        .requestMatchers("/me").authenticated()
                        .requestMatchers("/realms/**").hasAuthority(Role.SUPER_ADMIN.asSpringRole())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}