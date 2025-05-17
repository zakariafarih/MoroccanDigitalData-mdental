package org.mdental.authcore.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.config.KeycloakProperties;
import org.mdental.authcore.service.RealmTemplateService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClient {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;
    private final RealmTemplateService realmTemplateService;

    /**
     * Creates a new realm in Keycloak
     *
     * @param realmName The name of the realm to create
     * @param clinicSlug The clinic slug for customizing realm information
     * @return A map containing realm information (issuer, admin username, temp password)
     */
    public Map<String, String> createRealm(String realmName, String clinicSlug) {
        log.info("Creating new Keycloak realm: {}", realmName);

        // 1. Get admin token from master realm
        String adminToken = getAdminToken();

        // 2. Load and customize the realm template
        Map<String, String> variables = new HashMap<>();
        variables.put("REALM_NAME", realmName);
        variables.put("CLINIC_SLUG", clinicSlug);
        variables.put("ADMIN_ROLE", "CLINIC_ADMIN");

        String realmConfig = realmTemplateService.loadAndProcessTemplate(variables);

        // 3. Create the realm in Keycloak
        boolean created = createKeycloakRealm(adminToken, realmConfig);

        if (!created) {
            throw new RuntimeException("Failed to create realm in Keycloak");
        }

        // 4. Generate a temporary password for the realm admin
        String tmpPassword = UUID.randomUUID().toString().substring(0, 12);

        // 5. Return the realm details
        return Map.of(
                "issuer", String.format("%s/realms/%s", keycloakProperties.getBaseUrl(), realmName),
                "kcRealmAdminUser", "admin",
                "tmpPassword", tmpPassword
        );
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

        // 1. Get admin token from master realm
        String adminToken = getAdminToken();

        // 2. Generate a temporary password
        String tmpPassword = UUID.randomUUID().toString().substring(0, 12);

        // 3. Reset the user's password in Keycloak
        boolean reset = resetUserPassword(adminToken, realmName, adminUser, tmpPassword);

        if (!reset) {
            throw new RuntimeException("Failed to reset admin password in Keycloak");
        }

        // 4. Return the realm details
        return Map.of(
                "issuer", String.format("%s/realms/%s", keycloakProperties.getBaseUrl(), realmName),
                "realmName", realmName,
                "kcRealmAdminUser", adminUser,
                "tmpPassword", tmpPassword
        );
    }

    private String getAdminToken() {
        log.debug("Getting admin token from Keycloak");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        String body = String.format(
                "client_id=admin-cli&grant_type=password&username=%s&password=%s",
                keycloakProperties.getAdminUsername(), "********"); // Log masked password
        log.debug("Auth request body (redacted): {}", body);

        String actualBody = String.format(
                "client_id=admin-cli&grant_type=password&username=%s&password=%s",
                keycloakProperties.getAdminUsername(), keycloakProperties.getAdminPassword());

        HttpEntity<String> request = new HttpEntity<>(actualBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                keycloakProperties.getTokenUrl(),
                HttpMethod.POST,
                request,
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || !response.hasBody()) {
            throw new RuntimeException("Failed to get admin token from Keycloak");
        }

        return (String) response.getBody().get("access_token");
    }

    private String loadAndCustomizeRealmTemplate(String realm, String clinicSlug) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/realm-template.json");
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // Replace placeholders with actual values
            return template
                    .replace("__CLINIC_NAME__", clinicSlug)
                    .replace("mdental-__CLINIC_NAME__", realm)
                    .replace("\"name\": \"admin\"", "\"name\": \"CLINIC_ADMIN\"")
                    .replace("\"admin\"", "\"CLINIC_ADMIN\"");
        } catch (IOException e) {
            log.error("Failed to load realm template", e);
            throw new RuntimeException("Failed to load realm template", e);
        }
    }

    private boolean createKeycloakRealm(String adminToken, String realmConfig) {
        log.debug("Creating realm in Keycloak");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminToken);
        headers.add("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(realmConfig, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    keycloakProperties.getAdminUrl(),
                    HttpMethod.POST,
                    request,
                    Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to create realm in Keycloak", e);
            return false;
        }
    }

    private boolean resetUserPassword(String adminToken, String realm, String username, String newPassword) {
        log.debug("Resetting password for user {} in realm {}", username, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminToken);
        headers.add("Content-Type", "application/json");

        String userUrl = String.format("%s/%s/users", keycloakProperties.getAdminUrl(), realm);

        // 1. Get user ID by username
        ResponseEntity<List> usersResponse = restTemplate.exchange(
                userUrl + "?username=" + username + "&exact=true",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        if (!usersResponse.getStatusCode().is2xxSuccessful() ||
                usersResponse.getBody() == null ||
                usersResponse.getBody().isEmpty()) {
            log.error("User not found: {}", username);
            return false;
        }

        Map<String, Object> user = (Map<String, Object>) usersResponse.getBody().get(0);
        String userId = (String) user.get("id");

        // 2. Reset password
        Map<String, Object> passwordReset = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", true
        );

        try {
            ResponseEntity<Void> resetResponse = restTemplate.exchange(
                    userUrl + "/" + userId + "/reset-password",
                    HttpMethod.PUT,
                    new HttpEntity<>(passwordReset, headers),
                    Void.class);

            return resetResponse.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to reset password: {}", e.getMessage());
            return false;
        }
    }
}