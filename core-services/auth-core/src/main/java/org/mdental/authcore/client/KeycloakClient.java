package org.mdental.authcore.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.client.command.CreateRealmCommand;
import org.mdental.authcore.client.command.KeycloakCommand;
import org.mdental.authcore.client.command.RegeneratePasswordCommand;
import org.mdental.authcore.config.KeycloakProperties;
import org.mdental.authcore.exception.KeycloakException;
import org.mdental.authcore.service.RealmTemplateService;
import org.mdental.authcore.service.TokenCache;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;
    private final RealmTemplateService realmTemplateService;
    private final TokenCache tokenCache;

    /**
     * Creates a new realm in Keycloak
     *
     * @param realmName The name of the realm to create
     * @param clinicSlug The clinic slug for customizing realm information
     * @return A map containing realm information (issuer, admin username, temp password)
     */
    public Map<String, String> createRealm(String realmName, String clinicSlug) {
        log.info("Creating new Keycloak realm: {}", realmName);

        // Get admin token using the cache
        String adminToken = tokenCache.getSuperAdminToken();

        // Create realm command
        return executeCommand(new CreateRealmCommand(
                restTemplate,
                keycloakProperties,
                realmTemplateService,
                realmName,
                clinicSlug,
                adminToken
        ));
    }

    /**
     * Regenerates the admin password for a realm
     *
     * @param realmName The name of the realm
     * @param adminUser The admin username
     * @return A map containing realm information
     */
    public Map<String, String> regenerateAdminPassword(String realmName, String adminUser) {
        log.info("Regenerating admin password for realm: {}", realmName);

        // Get admin token using the cache
        String adminToken = tokenCache.getSuperAdminToken();

        // Regenerate password command
        return executeCommand(new RegeneratePasswordCommand(
                restTemplate,
                keycloakProperties,
                realmName,
                adminUser,
                adminToken
        ));
    }

    /**
     * Performs direct grant login (username/password) against Keycloak
     *
     * @param realm The realm name
     * @param username The username
     * @param password The password
     * @return Map containing access_token, refresh_token, etc.
     */
    public Map<String, Object> directGrantLogin(String realm, String username, String password) {
        log.debug("Performing direct grant login for user: {} in realm: {}", username, realm);

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakProperties.getBaseUrl(), realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "mdental-frontend");
        formData.add("username", username);
        formData.add("password", password);

        // Add client secret if this client is confidential
        if (isConfidentialClient(realm, "mdental-frontend")) {
            String clientSecret = getClientSecret(realm, "mdental-frontend");
            if (clientSecret != null && !clientSecret.isEmpty()) {
                log.debug("Adding client_secret for confidential client");
                formData.add("client_secret", clientSecret);
            }
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KeycloakException("Failed to authenticate user");
            }

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error during direct grant login", e);
            throw new KeycloakException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes an access token using a refresh token
     *
     * @param realm The realm name
     * @param refreshToken The refresh token
     * @return Map containing new access_token, refresh_token, etc.
     */
    public Map<String, Object> refreshToken(String realm, String refreshToken) {
        log.debug("Refreshing token in realm: {}", realm);

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakProperties.getBaseUrl(), realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", "mdental-frontend");
        formData.add("refresh_token", refreshToken);

        // Add client secret if this client is confidential
        if (isConfidentialClient(realm, "mdental-frontend")) {
            String clientSecret = getClientSecret(realm, "mdental-frontend");
            if (clientSecret != null && !clientSecret.isEmpty()) {
                log.debug("Adding client_secret for confidential client in refresh flow");
                formData.add("client_secret", clientSecret);
            }
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new KeycloakException("Failed to refresh token");
            }

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error during token refresh", e);
            throw new KeycloakException("Token refresh failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new user in Keycloak with an assigned role
     *
     * @param realm The realm name
     * @param username The username
     * @param email The email
     * @param firstName The first name
     * @param lastName The last name
     * @param password The password
     * @param role The role to assign (defaults to PATIENT if null)
     */
    public void createUser(String realm, String username, String email, String firstName, String lastName, String password, String role) {
        log.debug("Creating user: {} in realm: {} with role: {}", username, realm, role);

        String adminToken = tokenCache.getSuperAdminToken();
        String usersUrl = String.format("%s/%s/users", keycloakProperties.getAdminUrl(), realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", password);
        credentials.put("temporary", false);

        // Default to PATIENT if role is null or empty
        String userRole = (role == null || role.trim().isEmpty()) ? "PATIENT" : role;

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("enabled", true);
        userData.put("emailVerified", true);
        userData.put("credentials", Collections.singletonList(credentials));
        userData.put("realmRoles", Collections.singletonList(userRole));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    usersUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new KeycloakException("Failed to create user");
            }
        } catch (RestClientException e) {
            log.error("Error creating user", e);
            throw new KeycloakException("User creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Triggers a password reset email for a user
     *
     * @param realm The realm name
     * @param email The user's email
     */
    public void triggerResetEmail(String realm, String email) {
        log.debug("Triggering password reset for email: {} in realm: {}", email, realm);

        String adminToken = tokenCache.getSuperAdminToken();
        String resetUrl = String.format("%s/%s/users", keycloakProperties.getAdminUrl(), realm);

        // First, find the user by email
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            ResponseEntity<Object[]> response = restTemplate.exchange(
                    resetUrl + "?email=" + email + "&exact=true",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object[].class);

            if (!response.getStatusCode().is2xxSuccessful() ||
                    response.getBody() == null ||
                    response.getBody().length == 0) {
                log.warn("User with email {} not found in realm {}", email, realm);
                return; // Don't reveal if user exists or not
            }

            // User found, get the ID
            Map<String, Object> user = (Map<String, Object>) response.getBody()[0];
            String userId = (String) user.get("id");

            // Execute actions email to trigger reset
            Map<String, Object> actionsConfig = new HashMap<>();
            actionsConfig.put("lifespan", 43200); // 12 hours
            actionsConfig.put("actions", Collections.singletonList("UPDATE_PASSWORD"));

            HttpHeaders actionHeaders = new HttpHeaders();
            actionHeaders.setBearerAuth(adminToken);
            actionHeaders.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> actionRequest = new HttpEntity<>(actionsConfig, actionHeaders);

            // Add client_id query parameter as required in KC 24+
            String execActionUrl = resetUrl + "/" + userId + "/execute-actions-email?client_id=mdental-frontend";

            restTemplate.exchange(
                    execActionUrl,
                    HttpMethod.PUT,
                    actionRequest,
                    Void.class);

        } catch (RestClientException e) {
            log.error("Error triggering password reset", e);
            throw new KeycloakException("Password reset failed: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new OIDC client in the realm
     *
     * @param realm The realm name
     * @param clientId The client ID
     * @param clientName The client name
     * @param redirectUri The redirect URI (can use wildcards)
     * @param publicClient If true, client is public (no secret)
     * @return Map containing client information including secret for confidential clients
     */
    public Map<String, String> createClient(String realm, String clientId, String clientName, String redirectUri, boolean publicClient) {
        log.debug("Creating client: {} in realm: {}", clientId, realm);

        String adminToken = tokenCache.getSuperAdminToken();
        String clientsUrl = String.format("%s/%s/clients", keycloakProperties.getAdminUrl(), realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> clientData = new HashMap<>();
        clientData.put("clientId", clientId);
        clientData.put("name", clientName);
        clientData.put("enabled", true);
        clientData.put("redirectUris", Collections.singletonList(redirectUri));
        clientData.put("publicClient", publicClient);
        clientData.put("standardFlowEnabled", true);
        clientData.put("directAccessGrantsEnabled", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(clientData, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    clientsUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new KeycloakException("Failed to create client");
            }

            Map<String, String> result = new HashMap<>();
            result.put("clientId", clientId);

            // For confidential clients, generate and retrieve client secret
            if (!publicClient) {
                String clientSecret = generateClientSecret(realm, clientId, adminToken);
                result.put("clientSecret", clientSecret);
            }

            return result;
        } catch (RestClientException e) {
            log.error("Error creating client", e);
            throw new KeycloakException("Client creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a new client secret for a confidential client
     */
    private String generateClientSecret(String realm, String clientId, String adminToken) {
        // First, find the client by ID
        String clientsUrl = String.format("%s/%s/clients", keycloakProperties.getAdminUrl(), realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<Object[]> clientsResponse = restTemplate.exchange(
                clientsUrl + "?clientId=" + clientId + "&exact=true",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object[].class);

        if (clientsResponse.getBody() == null || clientsResponse.getBody().length == 0) {
            throw new KeycloakException("Client not found: " + clientId);
        }

        Map<String, Object> client = (Map<String, Object>) clientsResponse.getBody()[0];
        String id = (String) client.get("id");

        // Generate client secret
        HttpHeaders secretHeaders = new HttpHeaders();
        secretHeaders.setBearerAuth(adminToken);

        ResponseEntity<Map> secretResponse = restTemplate.exchange(
                clientsUrl + "/" + id + "/client-secret",
                HttpMethod.POST,
                new HttpEntity<>(secretHeaders),
                Map.class);

        if (secretResponse.getBody() == null) {
            throw new KeycloakException("Failed to generate client secret");
        }

        // Return the secret value
        Map<String, String> value = (Map<String, String>) secretResponse.getBody().get("value");
        return value.get("value");
    }

    /**
     * Check if a client is confidential (not public)
     */
    private boolean isConfidentialClient(String realm, String clientId) {
        try {
            String adminToken = tokenCache.getSuperAdminToken();
            String clientsUrl = String.format("%s/%s/clients", keycloakProperties.getAdminUrl(), realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            ResponseEntity<Object[]> response = restTemplate.exchange(
                    clientsUrl + "?clientId=" + clientId + "&exact=true",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object[].class);

            if (response.getBody() != null && response.getBody().length > 0) {
                Map<String, Object> client = (Map<String, Object>) response.getBody()[0];
                Object publicClient = client.get("publicClient");

                // If publicClient is false, it's a confidential client
                return publicClient != null && publicClient instanceof Boolean && !((Boolean) publicClient);
            }

            return false;
        } catch (Exception e) {
            log.warn("Error checking if client is confidential: {}", e.getMessage());
            return false; // Default to assuming it's public if we can't determine
        }
    }

    /**
     * Get the client secret for a client
     */
    private String getClientSecret(String realm, String clientId) {
        try {
            String adminToken = tokenCache.getSuperAdminToken();
            String clientsUrl = String.format("%s/%s/clients", keycloakProperties.getAdminUrl(), realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            ResponseEntity<Object[]> clientsResponse = restTemplate.exchange(
                    clientsUrl + "?clientId=" + clientId + "&exact=true",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object[].class);

            if (clientsResponse.getBody() == null || clientsResponse.getBody().length == 0) {
                return null;
            }

            Map<String, Object> client = (Map<String, Object>) clientsResponse.getBody()[0];
            String id = (String) client.get("id");

            // Get client secret
            ResponseEntity<Map> secretResponse = restTemplate.exchange(
                    clientsUrl + "/" + id + "/client-secret",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            if (secretResponse.getBody() == null) {
                return null;
            }

            // Return the secret value
            return (String) secretResponse.getBody().get("value");

        } catch (Exception e) {
            log.warn("Error getting client secret: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to execute a command and returns its result
     */
    private <T> T executeCommand(KeycloakCommand<T> command) {
        return command.execute();
    }
}