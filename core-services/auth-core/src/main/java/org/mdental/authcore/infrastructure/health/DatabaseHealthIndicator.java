package org.mdental.authcore.infrastructure.health;

import lombok.RequiredArgsConstructor;
import org.mdental.authcore.domain.repository.TenantRepository;
import org.mdental.authcore.infrastructure.tenant.TenantContext;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final TenantRepository tenantRepository;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            // Check if database is responsive with a simple query
            int result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != 1) {
                return Health.down().withDetail("database", "Failed simple connectivity test").build();
            }

            // Check if tenant filter is working
            try {
                // Check if tenant count query works
                long tenantCount = tenantRepository.count();
                builder.withDetail("tenantCount", tenantCount);

                // Check if tenant context is properly cleared
                if (TenantContext.getTenantId() != null) {
                    return Health.down()
                            .withDetail("tenantFilter", "Tenant context not properly cleared")
                            .build();
                }

                builder.withDetail("tenantFilter", "operational");

            } catch (Exception e) {
                return Health.down()
                        .withDetail("tenantFilter", "Failed: " + e.getMessage())
                        .build();
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Connectivity failed: " + e.getMessage())
                    .build();
        }
    }
}