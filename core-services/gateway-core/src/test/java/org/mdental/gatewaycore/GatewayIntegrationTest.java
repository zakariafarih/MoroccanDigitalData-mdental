package org.mdental.gatewaycore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.filter.ReactiveAuthFilter;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ReactiveAuthFilter reactiveAuthFilter;

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();

        // Setup mock JWT provider
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        AuthPrincipal principal = new AuthPrincipal(
                userId,
                tenantId,
                "test.user",
                "test@mdental.org",
                Set.of(Role.DOCTOR)
        );

        String mockToken = "test.jwt.token";
        when(jwtTokenProvider.extractTokenFromHeader(anyString())).thenReturn(Optional.of(mockToken));
        when(jwtTokenProvider.validateToken(mockToken)).thenReturn(true);
        when(jwtTokenProvider.parseToken(mockToken)).thenReturn(principal);
    }

    @Test
    void contextLoads() {
        // Verify application context loads successfully
    }

    @Test
    void fallbackControllerRespondsWithError() {
        webTestClient.get()
                .uri("/fallback/clinic")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    void authenticatedRequestShouldWork() {
        webTestClient.get()
                .uri("/api/clinics/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test.jwt.token")
                .exchange()
                .expectStatus().isNotFound(); // 404 because the route isn't actually implemented in tests
    }
}