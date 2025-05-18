package org.mdental.authcore.client.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.config.KeycloakProperties;
import org.mdental.authcore.service.RealmTemplateService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class CreateRealmCommand implements KeycloakCommand<Map<String, String>> {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;
    private final RealmTemplateService realmTemplateService;
    private final String realmName;
    private final String clinicSlug;
    private final String adminToken;

    @Override
    public Map<String, String> execute() {
        log.info("Executing CreateRealmCommand for realm: {}", realmName);

        // Prepare realm template with variables
        Map<String, String> variables = new HashMap<>();
        variables.put("REALM_NAME", realmName);
        variables.put("CLINIC_SLUG", clinicSlug);
        variables.put("ADMIN_ROLE", "CLINIC_ADMIN");

        String realmConfig = realmTemplateService.loadAndProcessTemplate(variables);

        // Create the realm in Keycloak
        boolean created = createKeycloakRealm(adminToken, realmConfig);

        if (!created) {
            throw new RuntimeException("Failed to create realm in Keycloak");
        }

        // Generate temporary password
        String tmpPassword = UUID.randomUUID().toString().substring(0, 12);

        // Return realm details
        return Map.of(
                "issuer", String.format("%s/realms/%s", keycloakProperties.getBaseUrl(), realmName),
                "kcRealmAdminUser", "admin",
                "tmpPassword", tmpPassword
        );
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
}