package org.mdental.authcore.client.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.config.KeycloakProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class AdminTokenCommand implements KeycloakCommand<String> {

    private final RestTemplate restTemplate;
    private final KeycloakProperties keycloakProperties;

    @Override
    public String execute() {
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
}