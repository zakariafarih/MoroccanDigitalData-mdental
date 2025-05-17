package org.mdental.authcore.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.model.dto.RealmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealmProvisioningService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.admin.url:https://auth.mdental.org/admin/realms}")
    private String keycloakAdminUrl;

    @Value("${keycloak.auth.url:https://auth.mdental.org/realms/master/protocol/openid-connect/token}")
    private String keycloakAuthUrl;

    @Value("${keycloak.realm.template.path:/opt/mdental/keycloak/realm-template.json}")
    private String realmTemplatePath;

    @Value("${KC_ADMIN_USER:admin}")
    private String kcAdminUser;

    @Value("${KC_ADMIN_PWD:admin}")
    private String kcAdminPwd;

    @Retry(name = "keycloak", fallbackMethod = "createRealmFallback")
    public RealmResponse createRealm(String realm, String clinicSlug) {
        log.info("Creating new Keycloak realm: {}", realm);

        // 1. Get admin token from master realm
        String adminToken = getAdminToken();

        // 2. Load and customize the realm template
        String realmConfig = loadAndCustomizeRealmTemplate(realm, clinicSlug);

        // 3. Create the realm in Keycloak
        boolean created = createKeycloakRealm(adminToken, realmConfig);

        if (!created) {
            throw new RuntimeException("Failed to create realm in Keycloak");
        }

        // 4. Generate a temporary password for the realm admin
        String tmpPassword = UUID.randomUUID().toString().substring(0, 8);

        // 5. Return the realm details
        return RealmResponse.builder()
                .issuer(String.format("https://auth.mdental.org/realms/%s", realm))
                .realmName(realm)
                .kcRealmAdminUser("admin")
                .tmpPassword(tmpPassword)
                .build();
    }

    public RealmResponse createRealmFallback(String realm, String clinicSlug, Exception ex) {
        log.error("Failed to create realm after retries: {}", ex.getMessage());
        throw new RuntimeException("Failed to create realm after multiple attempts", ex);
    }

    @Retry(name = "keycloak")
    private String getAdminToken() {
        log.debug("Getting admin token from Keycloak");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        String body = String.format(
                "client_id=admin-cli&grant_type=password&username=%s&password=%s",
                kcAdminUser, "********"); // Log masked password
        log.debug("Auth request body (redacted): {}", body);

        String actualBody = String.format(
                "client_id=admin-cli&grant_type=password&username=%s&password=%s",
                kcAdminUser, kcAdminPwd);

        HttpEntity<String> request = new HttpEntity<>(actualBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                keycloakAuthUrl,
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
            Path path = Paths.get(realmTemplatePath);
            String template = Files.readString(path);

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

    @Retry(name = "keycloak")
    private boolean createKeycloakRealm(String adminToken, String realmConfig) {
        log.debug("Creating realm in Keycloak");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminToken);
        headers.add("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(realmConfig, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    keycloakAdminUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to create realm in Keycloak", e);
            return false;
        }
    }

    @Retry(name = "keycloak", fallbackMethod = "regenerateAdminPasswordFallback")
    public RealmResponse regenerateAdminPassword(String realm, String adminUsername) {
        log.info("Regenerating admin password for realm: {}", realm);

        // 1. Get admin token from master realm
        String adminToken = getAdminToken();

        // 2. Generate a temporary password
        String tmpPassword = UUID.randomUUID().toString().substring(0, 12);

        // 3. Reset the user's password in Keycloak
        boolean reset = resetUserPassword(adminToken, realm, adminUsername, tmpPassword);

        if (!reset) {
            throw new RuntimeException("Failed to reset admin password in Keycloak");
        }

        // 4. Return the realm details
        return RealmResponse.builder()
                .issuer(String.format("https://auth.mdental.org/realms/%s", realm))
                .realmName(realm)
                .kcRealmAdminUser(adminUsername)
                .tmpPassword(tmpPassword)
                .build();
    }

    public RealmResponse regenerateAdminPasswordFallback(String realm, String adminUsername, Exception ex) {
        log.error("Failed to regenerate admin password after retries: {}", ex.getMessage());
        throw new RuntimeException("Failed to regenerate password after multiple attempts", ex);
    }

    @Retry(name = "keycloak")
    private boolean resetUserPassword(String adminToken, String realm, String username, String newPassword) {
        log.debug("Resetting password for user {} in realm {}", username, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminToken);
        headers.add("Content-Type", "application/json");

        String userUrl = String.format("%s/%s/users", keycloakAdminUrl, realm);

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