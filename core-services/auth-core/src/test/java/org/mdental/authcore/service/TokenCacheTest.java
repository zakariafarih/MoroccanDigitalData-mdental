package org.mdental.authcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.authcore.config.KeycloakProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCacheTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KeycloakProperties keycloakProperties;

    private TokenCache tokenCache;

    @BeforeEach
    void setUp() {
        when(keycloakProperties.getTokenUrl()).thenReturn("http://keycloak:8080/token");
        when(keycloakProperties.getAdminUsername()).thenReturn("admin");
        when(keycloakProperties.getAdminPassword()).thenReturn("admin");

        tokenCache = new TokenCache(restTemplate, keycloakProperties);
    }

    @Test
    void shouldFetchNewTokenWhenCacheEmpty() {
        // Arrange
        ResponseEntity<Map> response = new ResponseEntity<>(Map.of("access_token", "mock-token"), HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(response);

        // Act
        String token = tokenCache.getSuperAdminToken();

        // Assert
        assertThat(token).isEqualTo("mock-token");
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    void shouldReturnCachedTokenWhenValid() throws Exception {
        // Arrange
        ResponseEntity<Map> response = new ResponseEntity<>(Map.of("access_token", "mock-token"), HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(response);

        // First call to prime the cache
        tokenCache.getSuperAdminToken();

        // Act - Second call should use cached token
        String token = tokenCache.getSuperAdminToken();

        // Assert
        assertThat(token).isEqualTo("mock-token");
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    void shouldRefreshTokenWhenExpired() throws Exception {
        // Arrange
        ResponseEntity<Map> response1 = new ResponseEntity<>(Map.of("access_token", "token1"), HttpStatus.OK);
        ResponseEntity<Map> response2 = new ResponseEntity<>(Map.of("access_token", "token2"), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(response1, response2);

        // First call to prime the cache
        tokenCache.getSuperAdminToken();

        // Force expiry of the token
        Field expiryField = TokenCache.class.getDeclaredField("expiryTime");
        expiryField.setAccessible(true);
        expiryField.set(tokenCache, Instant.now().minusSeconds(10));

        // Act - Second call should get a new token
        String token = tokenCache.getSuperAdminToken();

        // Assert
        assertThat(token).isEqualTo("token2");
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        // Arrange
        ResponseEntity<Map> response = new ResponseEntity<>(Map.of("access_token", "token"), HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(response);

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Act - Multiple threads requesting token simultaneously
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    tokenCache.getSuperAdminToken();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // Assert - Should only make one REST call despite multiple threads
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }
}