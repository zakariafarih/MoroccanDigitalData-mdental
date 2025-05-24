package org.mdental.authcore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.domain.service.TenantService;
import org.mdental.authcore.web.dto.CreateTenantRequest;
import org.mdental.authcore.web.dto.CreateTenantWithAdminRequest;
import org.mdental.authcore.web.dto.TenantResponse;
import org.mdental.authcore.web.dto.TenantWithAdminResponse;
import org.mdental.authcore.web.mapper.TenantMapper;
import org.mdental.commons.model.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API for tenant management (super-admin only).
 */
@RestController
@RequestMapping("/internal/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Tenant Management", description = "Operations for managing tenants (super-admin only)")
public class InternalTenantController {
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;

    /**
     * Create a new tenant.
     *
     * @param request the tenant creation request
     * @return the created tenant
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Operation(summary = "Create tenant", description = "Creates a new tenant (clinic)")
    public ApiResponse<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        log.info("REST request to create tenant: {}", request.slug());
        Tenant tenant = tenantService.createTenant(request.slug(), request.name());
        return ApiResponse.success(tenantMapper.toTenantResponse(tenant));
    }

    /**
     * Create a new tenant with an admin user.
     *
     * @param request the tenant with admin creation request
     * @return the created tenant with admin credentials
     */
    @PostMapping("/with-admin")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Operation(summary = "Create tenant with admin", description = "Creates a new tenant with an admin user")
    public ApiResponse<TenantWithAdminResponse> createTenantWithAdmin(
            @Valid @RequestBody CreateTenantWithAdminRequest request) {
        log.info("REST request to create tenant with admin: {}", request.slug());

        TenantService.TenantWithAdmin result = tenantService.createTenantWithAdmin(
                request.slug(),
                request.name(),
                request.adminUsername(),
                request.adminEmail(),
                request.adminFirstName(),
                request.adminLastName()
        );

        TenantWithAdminResponse response = new TenantWithAdminResponse(
                tenantMapper.toTenantResponse(result.tenant()),
                request.adminUsername(),
                result.adminPassword()
        );

        return ApiResponse.success(response);
    }
}