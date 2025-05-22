package org.mdental.authcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.CreateTenantRequest;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.authcore.service.RealmProvisioningService;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tenants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Tenant Management", description = "Operations for managing tenants (super-admin only)")
public class InternalTenantController {

    private final RealmProvisioningService realmProvisioningService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + Role.SUPER_ADMIN_AUTHORITY + "')")
    @Operation(summary = "Create tenant", description = "Creates a new tenant (realm) for a clinic")
    public ApiResponse<RealmResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        log.info("REST request to create tenant: {}", request.getRealm());
        RealmResponse response = realmProvisioningService.createRealm(request.getRealm(), request.getClinicSlug());
        return ApiResponse.success(response);
    }
}