package org.mdental.authcore.application.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.AuthEvent;
import org.mdental.authcore.domain.model.Client;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.ClientRepository;
import org.mdental.authcore.domain.service.OutboxService;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.authcore.exception.AuthenticationException;
import org.mdental.authcore.infrastructure.security.TokenService;
import org.mdental.security.password.PasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for OAuth token operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {
    private final ClientRepository clientRepository;
    private final UserService userService;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AuthenticationService authenticationService;
    private final OutboxService outboxService;

    /**
     * Process an OAuth token request.
     *
     * @param grantType the grant type
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param username the username (for password grant)
     * @param password the password (for password grant)
     * @param refreshToken the refresh token (for refresh_token grant)
     * @param ipAddress the client IP address
     * @return token response
     */
    @Transactional
    public Map<String, Object> processTokenRequest(
            String grantType,
            String clientId,
            String clientSecret,
            String username,
            String password,
            String refreshToken,
            String ipAddress) {

        switch (grantType) {
            case "password":
                return handlePasswordGrant(clientId, clientSecret, username, password, ipAddress);
            case "refresh_token":
                return handleRefreshTokenGrant(clientId, clientSecret, refreshToken);
            case "client_credentials":
                return handleClientCredentialsGrant(clientId, clientSecret);
            default:
                throw new AuthenticationException("Unsupported grant type: " + grantType);
        }
    }

    /**
     * Handle password grant type.
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param username the username
     * @param password the password
     * @param ipAddress the client IP address
     * @return token response
     */
    private Map<String, Object> handlePasswordGrant(
            String clientId,
            String clientSecret,
            String username,
            String password,
            String ipAddress) {

        // Validate client if provided
        if (clientId != null && !clientId.isBlank()) {
            validateClient(clientId, clientSecret, "password");
        }

        // Validate username and password
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Username and password are required for password grant");
        }

        // Extract tenant ID from username if in format username@tenant
        UUID tenantId = null;
        if (username.contains("@")) {
            String[] parts = username.split("@", 2);
            username = parts[0];
            // Look up tenant by slug
            // This is a simplified approach; in a real system you'd have a more robust way to determine tenant
        }

        // Authenticate user
        return authenticationService.authenticate(tenantId, username, password, ipAddress);
    }

    /**
     * Handle refresh token grant type.
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param refreshToken the refresh token
     * @return token response
     */
    private Map<String, Object> handleRefreshTokenGrant(String clientId, String clientSecret, String refreshToken) {
        // Validate client if provided
        if (clientId != null && !clientId.isBlank()) {
            validateClient(clientId, clientSecret, "refresh_token");
        }

        // Validate refresh token
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationException("Refresh token is required for refresh_token grant");
        }

        // Refresh token
        return authenticationService.refreshToken(refreshToken);
    }

    /**
     * Handle client credentials grant type.
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @return token response
     */
    private Map<String, Object> handleClientCredentialsGrant(String clientId, String clientSecret) {
        // Validate client
        Client client = validateClient(clientId, clientSecret, "client_credentials");

        // Generate access token for client
        String accessToken = tokenService.generateClientToken(client);

        // Track metrics and publish event
        outboxService.saveEvent(
                "Auth",
                client.getId(),
                AuthEvent.LOGIN_SUCCESS.name(),
                null,
                Map.of(
                        "clientId", client.getClientId(),
                        "tenantId", client.getTenantId() != null ? client.getTenantId() : "null",
                        "timestamp", Instant.now()
                )
        );

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", client.getAccessTokenValiditySeconds() != null
                ? client.getAccessTokenValiditySeconds() : 3600);

        return response;
    }

    /**
     * Validate a client.
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param grantType the grant type
     * @return the validated client
     */
    private Client validateClient(String clientId, String clientSecret, String grantType) {
        // Find client
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new AuthenticationException("Invalid client credentials"));

        // Check if client is active
        if (!client.isActive()) {
            throw new AuthenticationException("Client is disabled");
        }

        // Check if client supports the grant type
        if (!client.getAuthorizedGrantTypes().contains(grantType)) {
            throw new AuthenticationException("Unauthorized grant type: " + grantType);
        }

        // Verify client secret
        if (clientSecret == null || !passwordService.matches(clientSecret.toCharArray(), client.getSecretHash())) {
            throw new AuthenticationException("Invalid client credentials");
        }

        return client;
    }
}