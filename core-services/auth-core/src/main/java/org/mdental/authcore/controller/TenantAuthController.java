package org.mdental.authcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.*;
import org.mdental.authcore.service.TenantAuthService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/{tenant}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Authentication", description = "Authentication operations for tenants")
public class TenantAuthController {

    private final TenantAuthService tenantAuthService;

    @PostMapping("/login")
    @Operation(summary = "Login with username/password", description = "Authenticates a user with username and password")
    public ResponseEntity<Map<String, Object>> login(
            @PathVariable String tenant,
            @Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {} for tenant: {}", request.getUsername(), tenant);
        Map<String, Object> tokens = tenantAuthService.login(tenant, request.getUsername(), request.getPassword());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Refreshes access and refresh tokens using a valid refresh token")
    public ResponseEntity<Map<String, Object>> refresh(
            @PathVariable String tenant,
            @Valid @RequestBody RefreshRequest request) {
        log.info("REST request to refresh token for tenant: {}", tenant);
        Map<String, Object> tokens = tenantAuthService.refreshToken(tenant, request.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Registers a new user in the tenant's realm")
    public ApiResponse<Void> register(
            @PathVariable String tenant,
            @Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {} for tenant: {}", request.getUsername(), tenant);
        tenantAuthService.registerUser(
                tenant,
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword(),
                request.getRole()
        );
        return ApiResponse.success(null);
    }

    @PostMapping("/recover")
    @Operation(summary = "Request password recovery", description = "Sends a password reset email to the user")
    public ResponseEntity<ApiResponse<Void>> recover(
            @PathVariable String tenant,
            @Valid @RequestBody RecoverRequest request) {
        log.info("REST request to recover password for email: {} in tenant: {}", request.getEmail(), tenant);
        tenantAuthService.triggerPasswordReset(tenant, request.getEmail());
        // Always return 204 even if email not found to prevent user enumeration
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns information about the authenticated user")
    public ApiResponse<UserInfoResponse> me(
            @PathVariable String tenant,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("REST request to get current user info for tenant: {}", tenant);
        return ApiResponse.success(tenantAuthService.getCurrentUser(jwt));
    }
}