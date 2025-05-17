package org.mdental.authcore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String baseUrl = "https://auth.mdental.org";
    private String adminUrl = "https://auth.mdental.org/admin/realms";
    private String tokenUrl = "https://auth.mdental.org/realms/master/protocol/openid-connect/token";
    private String adminUsername = "admin";
    private String adminPassword = "admin";
}