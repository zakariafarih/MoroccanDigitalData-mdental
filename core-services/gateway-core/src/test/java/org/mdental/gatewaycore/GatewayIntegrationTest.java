package org.mdental.gatewaycore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();

        // Mock JWT decoder for tests requiring JWT
        Jwt jwt = createMockJwt();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
    }

    @Test
    void contextLoads() {
        // Verify application context loads successfully
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_api"})
    void fallbackControllerRespondsWithError() {
        webTestClient.get()
                .uri("/fallback/clinic")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.error.message").isNotEmpty();
    }

    private Jwt createMockJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");
        claims.put("scope", "api");
        claims.put("preferred_username", "test.user");
        claims.put("clinicId", "clinic-456");
        claims.put("email", "test@example.com");

        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(claimsMap -> claimsMap.putAll(claims))
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}