package org.mdental.authcore.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.authcore.config.KeycloakProperties;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeycloakProperties keycloakProperties;

    @InjectMocks
    private KeycloakClient keycloakClient;

    @BeforeEach
    void setUp() {
        when(keycloakProperties.getBaseUrl()).thenReturn("http://localhost:9080");
        when(keycloakProperties.getAdminUrl()).thenReturn("http://localhost:9080/admin/realms");
        when(keycloakProperties.getTokenUrl()).thenReturn("http://localhost:9080/realms/master/protocol/openid-connect/token");
        when(keycloakProperties.getAdminUsername()).thenReturn("admin");
        when(keycloakProperties.getAdminPassword()).thenReturn("admin");
    }

    @Test
    void createRealm_success() {
        // Mock token response
        Map<String, Object> tokenResponse = Map.of("access_token", "mock-token");
        when(restTemplate.exchange(
                eq("http://localhost:9080/realms/master/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock realm creation response
        when(restTemplate.exchange(
                eq("http://localhost:8080/admin/realms"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        // Test
        Map<String, String> result = keycloakClient.createRealm("mdental-test", "test");

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.get("issuer")).isEqualTo("http://localhost:8080/realms/mdental-test");
        assertThat(result.get("kcRealmAdminUser")).isEqualTo("admin");
        assertThat(result.get("tmpPassword")).isNotBlank();
    }

    @Test
    void createRealm_failsWhenTokenRequestFails() {
        // Mock token request failure
        when(restTemplate.exchange(
                eq("http://localhost:8080/realms/master/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

        // Test & verify
        assertThrows(RuntimeException.class, () ->
                keycloakClient.createRealm("mdental-test", "test"));
    }

    @Test
    void regenerateAdminPassword_success() {
        // Mock token response
        Map<String, Object> tokenResponse = Map.of("access_token", "mock-token");
        when(restTemplate.exchange(
                eq("http://localhost:8080/realms/master/protocol/openid-connect/token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // Mock user search response
        List<Map<String, Object>> users = List.of(Map.of("id", "user-123"));
        when(restTemplate.exchange(
                contains("/users?username="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(List.class)
        )).thenReturn(new ResponseEntity<>(users, HttpStatus.OK));

        // Mock password reset response
        when(restTemplate.exchange(
                contains("/users/user-123/reset-password"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        // Test
        Map<String, String> result = keycloakClient.regenerateAdminPassword("mdental-test", "admin");

        // Verify
        assertThat(result).isNotNull();
        assertThat(result.get("issuer")).isEqualTo("http://localhost:8080/realms/mdental-test");
        assertThat(result.get("kcRealmAdminUser")).isEqualTo("admin");
        assertThat(result.get("tmpPassword")).isNotBlank();
    }
}