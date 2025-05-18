package org.mdental.authcore.client.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.config.KeycloakProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RegeneratePasswordCommand implements KeycloakCommand<Map<String, String>> {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;
    private final String realmName;
    private final String adminUser;
    private final String adminToken;

    @Override
    public Map<String, String> execute() {
        log.info("Executing RegeneratePasswordCommand for realm: {} and user: {}", realmName, adminUser);

        // Generate temporary password
        String tmpPassword = UUID.randomUUID().toString().substring(0, 12);

        // Reset the user's password
        boolean reset = resetUserPassword(adminToken, realmName, adminUser, tmpPassword);

        if (!reset) {
            throw new RuntimeException("Failed to reset admin password in Keycloak");
        }

        // Return realm details
        return Map.of(
                "issuer", String.format("%s/realms/%s", keycloakProperties.getBaseUrl(), realmName),
                "realmName", realmName,
                "kcRealmAdminUser", adminUser,
                "tmpPassword", tmpPassword
        );
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