package org.mdental.cliniccore.config;

import org.mdental.security.auditing.JpaAuditingConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(JpaAuditingConfig.class)
public class AuditingConfiguration {
    // Import centralized JpaAuditingConfig from security-commons
}