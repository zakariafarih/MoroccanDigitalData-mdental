package org.mdental.authcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.CreateRealmRequest;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.authcore.api.dto.RegeneratePasswordRequest;
import org.mdental.authcore.service.RealmProvisioningService;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/realms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Realm Management", description = "Operations for managing Keycloak realms")
public class RealmAdminController {

    private final RealmProvisioningService realmProvisioningService;

    @PostMapping
    @PreAuthorize("hasAuthority('" + Role.SUPER_ADMIN_AUTHORITY + "')")
    @Operation(summary = "Create realm", description = "Creates a new Keycloak realm for a clinic")
    public ApiResponse<RealmResponse> createRealm(@Valid @RequestBody CreateRealmRequest request) {
        log.info("REST request to create realm: {}", request.getRealm());
        RealmResponse response = realmProvisioningService.createRealm(request.getRealm(), request.getClinicSlug());
        return ApiResponse.success(response);
    }

    @PostMapping("/{realm}/regenerate-password")
    @PreAuthorize("hasAuthority('" + Role.SUPER_ADMIN_AUTHORITY + "')")
    @Operation(summary = "Regenerate password", description = "Regenerates the admin password for a realm")
    public ApiResponse<RealmResponse> regenerateAdminPassword(
            @PathVariable String realm,
            @Valid @RequestBody RegeneratePasswordRequest request) {
        log.info("REST request to regenerate admin password for realm: {}", realm);
        RealmResponse response = realmProvisioningService.regenerateAdminPassword(realm, request.getAdminUsername());
        return ApiResponse.success(response);
    }
}