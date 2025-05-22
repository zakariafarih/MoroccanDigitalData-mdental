package org.mdental.authcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.client.command.AdminTokenCommand;
import org.mdental.authcore.config.KeycloakProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCache {
    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;

    private String cachedToken;
    private Instant expiryTime;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Gets a valid super admin token, using a cached token if it's not expired
     * or refreshing if it's about to expire
     *
     * @return A valid Keycloak admin token
     */
    public String getSuperAdminToken() {
        // First try with read lock for better concurrency
        lock.readLock().lock();
        try {
            if (isTokenValid()) {
                return cachedToken;
            }
        } finally {
            lock.readLock().unlock();
        }

        // If token is invalid, acquire write lock to update it
        lock.writeLock().lock();
        try {
            // Check again in case another thread updated while we were waiting
            if (isTokenValid()) {
                return cachedToken;
            }

            log.debug("Refreshing Keycloak admin token");
            String newToken = new AdminTokenCommand(restTemplate, keycloakProperties).execute();

            // Set expiry to 60 seconds before actual expiry (typical JWT expiry is 300s)
            this.cachedToken = newToken;
            this.expiryTime = Instant.now().plusSeconds(240); // 5 minutes - 60 seconds

            return newToken;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null && expiryTime != null && Instant.now().isBefore(expiryTime);
    }
}