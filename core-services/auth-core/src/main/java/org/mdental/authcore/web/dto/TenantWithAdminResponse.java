package org.mdental.authcore.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for tenant with admin credentials.
 */
@Schema(description = "Tenant with admin credentials")
public record TenantWithAdminResponse(
        @Schema(description = "Tenant information")
        TenantResponse tenant,

        @Schema(description = "Admin username", example = "admin.nyc")
        String adminUsername,

        @Schema(description = "Temporary admin password", example = "P@ssw0rd")
        String temporaryPassword
) {}