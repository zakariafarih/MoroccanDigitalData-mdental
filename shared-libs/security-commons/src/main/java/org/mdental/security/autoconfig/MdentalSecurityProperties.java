package org.mdental.security.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@ConfigurationProperties(prefix = "mdental.auth")
@Getter @Setter
@Validated
public class MdentalSecurityProperties {
    /**
     * Comma-separated list of issuer patterns that are allowed
     * e.g.  https://auth.mdental.org/realms/platform,https://auth.mdental.org/realms/mdental-
     */
    @NotEmpty(message = "At least one allowed issuer pattern must be configured")
    private List<String> allowedIssuerPatterns = List.of();
}