package org.mdental.gatewaycore.config;

import org.junit.jupiter.api.Test;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.filter.ReactiveAuthFilter;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {})
@Import({MdentalSecurityConfig.class})
@ActiveProfiles("test")
class MdentalSecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveAuthFilter reactiveAuthFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void publicEndpointsShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/fallback/service")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/auth/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedEndpointsShouldRequireAuthentication() {
        webTestClient.get()
                .uri("/api/clinics")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpointsShouldBeAccessibleWithValidAuthentication() {
        AuthPrincipal principal = new AuthPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test.user",
                "test@mdental.org",
                Set.of(Role.DOCTOR)
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get()
                .uri("/api/clinics")
                .exchange()
                .expectStatus().isNotFound(); // 404 because route doesn't exist in test context
    }
}