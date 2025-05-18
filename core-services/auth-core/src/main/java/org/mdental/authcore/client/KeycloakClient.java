package org.mdental.authcore.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.client.command.AdminTokenCommand;
import org.mdental.authcore.client.command.CreateRealmCommand;
import org.mdental.authcore.client.command.KeycloakCommand;
import org.mdental.authcore.client.command.RegeneratePasswordCommand;
import org.mdental.authcore.config.KeycloakProperties;
import org.mdental.authcore.service.RealmTemplateService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

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

        // Get admin token
        String adminToken = executeCommand(new AdminTokenCommand(restTemplate, keycloakProperties));

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

        // Get admin token
        String adminToken = executeCommand(new AdminTokenCommand(restTemplate, keycloakProperties));

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
     * Executes a command and returns its result
     */
    private <T> T executeCommand(KeycloakCommand<T> command) {
        return command.execute();
    }
}