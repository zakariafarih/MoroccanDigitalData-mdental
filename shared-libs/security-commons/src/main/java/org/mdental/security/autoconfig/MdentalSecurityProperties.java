package org.mdental.security.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "mdental.auth")
@Getter @Setter
public class MdentalSecurityProperties {
    /**
     * Comma-separated list of issuer patterns that are allowed
     * e.g.  https://auth.mdental.org/realms/platform,https://auth.mdental.org/realms/mdental-
     */
    private List<String> allowedIssuerPatterns = List.of();
}
