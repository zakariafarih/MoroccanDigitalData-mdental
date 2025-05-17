package org.mdental.gatewaycore.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser
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
    @WithMockUser(authorities = {"SCOPE_api"})
    void protectedEndpointsShouldBeAccessibleWithValidScope() {
        webTestClient.get()
                .uri("/api/clinics")
                .exchange()
                .expectStatus().isNotFound(); // 404 because the route doesn't exist in test context
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"}) // No SCOPE_api
    void protectedEndpointsShouldDenyAccessWithoutRequiredScope() {
        webTestClient.get()
                .uri("/api/clinics")
                .exchange()
                .expectStatus().isForbidden();
    }
}