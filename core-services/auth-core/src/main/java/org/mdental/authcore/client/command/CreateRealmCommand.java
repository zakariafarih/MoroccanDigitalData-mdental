package org.mdental.authcore.client.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.config.KeycloakProperties;
import org.mdental.authcore.service.PasswordGenerator;
import org.mdental.authcore.service.RealmTemplateService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class CreateRealmCommand implements KeycloakCommand<Map<String, String>> {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;
    private final RealmTemplateService realmTemplateService;
    private final String realmName;
    private final String clinicSlug;
    private final String adminToken;
    private final PasswordGenerator passwordGenerator;

    @Override
    public Map<String, String> execute() {
        log.info("Executing CreateRealmCommand for realm: {}", realmName);

        // Generate temporary password for admin user
        String tmpPassword = passwordGenerator.generateStrongPassword(16);
        log.debug("Generated temporary admin password for realm {}", realmName);

        // Prepare realm template with variables
        Map<String, String> variables = new HashMap<>();
        variables.put("REALM_NAME", realmName);
        variables.put("CLINIC_SLUG", clinicSlug);
        variables.put("ADMIN_ROLE", "CLINIC_ADMIN");
        variables.put("ADMIN_PASSWORD", tmpPassword);

        String realmConfig = realmTemplateService.loadAndProcessTemplate(variables);

        // Create the realm in Keycloak
        boolean created = createKeycloakRealm(adminToken, realmConfig);

        if (!created) {
            throw new RuntimeException("Failed to create realm in Keycloak");
        }

        // Create frontend client
        Map<String, String> clientInfo = createFrontendClient(realmName);

        // Return realm details including client info
        Map<String, String> result = new HashMap<>();
        result.put("issuer", String.format("%s/realms/%s", keycloakProperties.getBaseUrl(), realmName));
        result.put("realmName", realmName);
        result.put("kcRealmAdminUser", "admin");
        result.put("tmpPassword", tmpPassword);
        result.put("serviceClientId", clientInfo.get("clientId"));
        result.put("serviceClientSecret", clientInfo.get("clientSecret"));

        return result;
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

    private Map<String, String> createFrontendClient(String realmName) {
        log.debug("Creating frontend client in realm: {}", realmName);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminToken);
        headers.add("Content-Type", "application/json");

        // Create client data
        Map<String, Object> clientData = new HashMap<>();
        clientData.put("clientId", "mdental-frontend");
        clientData.put("name", "MDental Frontend Application");
        clientData.put("enabled", true);
        clientData.put("redirectUris", java.util.Collections.singletonList("*"));
        clientData.put("publicClient", false); // Confidential client
        clientData.put("standardFlowEnabled", true);
        clientData.put("directAccessGrantsEnabled", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(clientData, headers);

        try {
            // Create the client
            String clientsUrl = String.format("%s/%s/clients", keycloakProperties.getAdminUrl(), realmName);
            ResponseEntity<Void> createResponse = restTemplate.exchange(
                    clientsUrl,
                    HttpMethod.POST,
                    request,
                    Void.class);

            if (!createResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to create client mdental-frontend");
            }

            // Find the created client ID
            ResponseEntity<Object[]> clientsResponse = restTemplate.exchange(
                    clientsUrl + "?clientId=mdental-frontend",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Object[].class);

            if (clientsResponse.getBody() == null || clientsResponse.getBody().length == 0) {
                throw new RuntimeException("Could not find created client mdental-frontend");
            }

            Map<String, Object> client = (Map<String, Object>) clientsResponse.getBody()[0];
            String id = (String) client.get("id");

            // Generate and get client secret
            ResponseEntity<Map> secretResponse = restTemplate.exchange(
                    clientsUrl + "/" + id + "/client-secret",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    Map.class);

            if (secretResponse.getBody() == null) {
                throw new RuntimeException("Failed to generate client secret");
            }

            Map<String, String> clientInfo = new HashMap<>();
            clientInfo.put("clientId", "mdental-frontend");

            // Extract the secret value
            Map<String, String> secretValue = (Map<String, String>) secretResponse.getBody().get("value");
            clientInfo.put("clientSecret", secretValue.get("value"));

            return clientInfo;
        } catch (Exception e) {
            log.error("Failed to create frontend client", e);
            throw new RuntimeException("Failed to create frontend client: " + e.getMessage(), e);
        }
    }
}