package org.mdental.authcore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.service.AuthService;
import org.mdental.authcore.domain.service.TenantService;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.authcore.domain.service.VerificationService;

import org.mdental.authcore.web.dto.*;
import org.mdental.authcore.web.mapper.UserMapper;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Controller for tenant-specific authentication operations.
 */
@RestController
@RequestMapping("/auth/{tenant}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tenant Authentication", description = "Authentication operations for tenants")
public class TenantAuthController {
    private final AuthService authService;
    private final TenantService tenantService;
    private final UserService userService;
    private final VerificationService verificationService;
    private final UserMapper userMapper;

    @Value("${mdental.auth.cookie.enabled:true}")
    private boolean cookieEnabled;

    @Value("${mdental.auth.cookie.domain:}")
    private String cookieDomain;

    @Value("${mdental.auth.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Login with username and password.
     *
     * @param tenant the tenant slug
     * @param request the login request
     * @param httpRequest the HTTP request
     * @param response the HTTP response
     * @return JWT tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a user with username and password")
    public ResponseEntity<Map<String, Object>> login(
            @PathVariable String tenant,
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        log.info("REST request to login user: {} for tenant: {}", request.username(), tenant);

        Tenant tenantEntity = tenantService.getTenantBySlug(tenant);
        String ipAddress = getClientIp(httpRequest);

        Map<String, Object> tokens = authService.authenticate(
                tenantEntity.getId(),
                request.username(),
                request.password(),
                ipAddress
        );

        // Set cookies if enabled
        if (cookieEnabled) {
            String accessToken = (String) tokens.get("access_token");
            String refreshToken = (String) tokens.get("refresh_token");

            setTokenCookies(response, accessToken, refreshToken);
        }

        return ResponseEntity.ok(tokens);
    }

    /**
     * Refresh access token using a refresh token.
     *
     * @param tenant the tenant slug
     * @param request the refresh request
     * @param response the HTTP response
     * @return new JWT tokens
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refreshes access token using a refresh token")
    public ResponseEntity<Map<String, Object>> refresh(
            @PathVariable String tenant,
            @Valid @RequestBody RefreshRequest request,
            HttpServletResponse response) {
        log.info("REST request to refresh token for tenant: {}", tenant);

        // Refresh token
        Map<String, Object> tokens = authService.refreshToken(request.refreshToken());

        // Set cookies if enabled
        if (cookieEnabled) {
            String accessToken = (String) tokens.get("access_token");
            String refreshToken = (String) tokens.get("refresh_token");

            setTokenCookies(response, accessToken, refreshToken);
        }

        return ResponseEntity.ok(tokens);
    }

    /**
     * Register a new user.
     *
     * @param tenant the tenant slug
     * @param request the registration request
     * @return empty success response
     */
    @PostMapping("/register")
    @Operation(summary = "Register", description = "Registers a new user")
    public ApiResponse<Void> register(
            @PathVariable String tenant,
            @Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {} for tenant: {}", request.username(), tenant);

        Tenant tenantEntity = tenantService.getTenantBySlug(tenant);

        // Determine role (default to PATIENT if not specified)
        String roleStr = request.role() != null ? request.role() : "PATIENT";
        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            role = Role.PATIENT;
        }

        User user = User.builder()
                .tenantId(tenantEntity.getId())
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .emailVerified(false)
                .roles(Set.of(role))
                .build();

        user = userService.createUser(user, request.password());

        // Send verification email
        verificationService.createEmailVerificationToken(user);

        return ApiResponse.success(null);
    }

    /**
     * Verify email with token.
     *
     * @param tenant the tenant slug
     * @param token the verification token
     * @return success response
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verifies user email using token from email")
    public ApiResponse<Void> verifyEmail(
            @PathVariable String tenant,
            @RequestParam String token) {
        log.info("REST request to verify email with token for tenant: {}", tenant);

        verificationService.verifyEmail(token);

        return ApiResponse.success(null);
    }

    /**
     * Request password reset.
     *
     * @param tenant the tenant slug
     * @param request the password reset request
     * @return empty response (always 204 to prevent user enumeration)
     */
    @PostMapping("/forgot")
    @Operation(summary = "Forgot password", description = "Initiates password reset process")
    public ResponseEntity<Void> forgotPassword(
            @PathVariable String tenant,
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("REST request to reset password for email: {} in tenant: {}", request.email(), tenant);

        Tenant tenantEntity = tenantService.getTenantBySlug(tenant);

        // Find user and send reset email if found
        userService.findByEmailAndTenantId(request.email(), tenantEntity.getId())
                .ifPresent(verificationService::createPasswordResetToken);

        // Always return 204 to prevent user enumeration
        return ResponseEntity.noContent().build();
    }

    /**
     * Reset password using token.
     *
     * @param tenant the tenant slug
     * @param request the password reset request
     * @return empty success response
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Resets password using token from email")
    public ApiResponse<Void> resetPassword(
            @PathVariable String tenant,
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("REST request to reset password with token for tenant: {}", tenant);

        verificationService.resetPassword(request.token(), request.newPassword());

        return ApiResponse.success(null);
    }

    /**
     * Logout (revoke refresh token).
     *
     * @param tenant the tenant slug
     * @param request the refresh request
     * @param response the HTTP response
     * @return empty response
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token")
    public ResponseEntity<Void> logout(
            @PathVariable String tenant,
            @Valid @RequestBody RefreshRequest request,
            HttpServletResponse response) {
        log.info("REST request to logout from tenant: {}", tenant);

        authService.revokeToken(request.refreshToken());

        // Clear cookies if enabled
        if (cookieEnabled) {
            clearTokenCookies(response);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user information.
     *
     * @param tenant the tenant slug
     * @param principal the authenticated principal
     * @return user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns information about the authenticated user")
    public ApiResponse<UserInfoResponse> me(
            @PathVariable String tenant,
            @AuthenticationPrincipal User principal) {
        log.debug("REST request to get current user info for tenant: {}", tenant);
        return ApiResponse.success(userMapper.toUserInfoResponse(principal));
    }

    /**
     * Update user profile.
     *
     * @param tenant the tenant slug
     * @param principal the authenticated principal
     * @param request the profile update request
     * @return updated user information
     */
    @PatchMapping("/me")
    @Operation(summary = "Update profile", description = "Updates user profile information")
    public ApiResponse<UserInfoResponse> updateProfile(
            @PathVariable String tenant,
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("REST request to update profile for user: {}", principal.getUsername());

        User updatedUser = userService.updateProfile(
                principal.getId(),
                request.firstName(),
                request.lastName(),
                request.email()
        );

        return ApiResponse.success(userMapper.toUserInfoResponse(updatedUser));
    }

    /**
     * Change password.
     *
     * @param tenant the tenant slug
     * @param principal the authenticated principal
     * @param request the password change request
     * @return empty success response
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes user password")
    public ApiResponse<Void> changePassword(
            @PathVariable String tenant,
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password for user: {}", principal.getUsername());

        userService.updatePassword(
                principal.getId(),
                request.currentPassword(),
                request.newPassword()
        );

        return ApiResponse.success(null);
    }

    /**
     * Extract client IP address from request.
     *
     * @param request the HTTP request
     * @return the client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Set token cookies.
     *
     * @param response the HTTP response
     * @param accessToken the access token
     * @param refreshToken the refresh token
     */
    private void setTokenCookies(HttpServletResponse response,
                                 String accessToken,
                                 String refreshToken) {
        // 1) Prepare builders with secure attributes
        ResponseCookie.ResponseCookieBuilder accessBuilder = ResponseCookie
                .from("__Secure-ACCESS_TOKEN", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(3600); // Match the JWT expiration time

        ResponseCookie.ResponseCookieBuilder refreshBuilder = ResponseCookie
                .from("__Secure-REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth") // Scope to auth paths only
                .sameSite("Strict")
                .maxAge(86400 * 30); // 30 days - match refresh token validity

        // 2) Conditionally apply domain
        if (!cookieDomain.isBlank()) {
            accessBuilder = accessBuilder.domain(cookieDomain);
            refreshBuilder = refreshBuilder.domain(cookieDomain);
        }

        // 3) Build cookies
        ResponseCookie accessCookie = accessBuilder.build();
        ResponseCookie refreshCookie = refreshBuilder.build();

        // 4) Add headers
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    /**
     * Clear token cookies.
     *
     * @param response the HTTP response
     */
    private void clearTokenCookies(HttpServletResponse response) {
        // Use the same secure attributes when clearing cookies
        ResponseCookie.ResponseCookieBuilder accessBuilder = ResponseCookie
                .from("__Secure-ACCESS_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0);  // Expire immediately

        ResponseCookie.ResponseCookieBuilder refreshBuilder = ResponseCookie
                .from("__Secure-REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/auth")
                .sameSite("Strict")
                .maxAge(0);  // Expire immediately

        if (!cookieDomain.isBlank()) {
            accessBuilder = accessBuilder.domain(cookieDomain);
            refreshBuilder = refreshBuilder.domain(cookieDomain);
        }

        ResponseCookie accessCookie = accessBuilder.build();
        ResponseCookie refreshCookie = refreshBuilder.build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}