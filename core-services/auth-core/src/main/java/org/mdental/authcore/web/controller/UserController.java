package org.mdental.authcore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.service.UserService;
import org.mdental.authcore.web.dto.UserInfoResponse;
import org.mdental.authcore.web.dto.UserLockedRequest;
import org.mdental.authcore.web.mapper.UserMapper;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Operations for managing users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Get user by ID.
     *
     * @param id the user ID
     * @param principal the authenticated principal
     * @return the user information
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_CLINIC_ADMIN') or #id == principal.id")
    @Operation(summary = "Get user", description = "Retrieves a user by ID")
    public ApiResponse<UserInfoResponse> getUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User principal) {
        log.debug("REST request to get user: {}", id);

        User user = userService.getUserById(id);

        // Verify tenant access
        if (!principal.getRoles().contains(Role.SUPER_ADMIN) &&
                !user.getTenantId().equals(principal.getTenantId())) {
            log.warn("User {} attempted to access user {} from different tenant",
                    principal.getUsername(), id);
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        return ApiResponse.success(userMapper.toUserInfoResponse(user));
    }

    /**
     * Lock or unlock a user account.
     *
     * @param id the user ID
     * @param request the lock/unlock request
     * @param principal the authenticated principal
     * @return the updated user information
     */
    @PatchMapping("/{id}/locked")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_CLINIC_ADMIN')")
    @Operation(summary = "Lock or unlock user", description = "Locks or unlocks a user account")
    public ApiResponse<UserInfoResponse> setLocked(
            @PathVariable UUID id,
            @Valid @RequestBody UserLockedRequest request,
            @AuthenticationPrincipal User principal) {
        log.info("REST request to set user locked status: {} to {}", id, request.locked());

        User user = userService.getUserById(id);

        // Verify tenant access
        if (!principal.getRoles().contains(Role.SUPER_ADMIN) &&
                !user.getTenantId().equals(principal.getTenantId())) {
            log.warn("User {} attempted to modify user {} from different tenant",
                    principal.getUsername(), id);
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        // Prevent locking super admin accounts
        if (user.getRoles().contains(Role.SUPER_ADMIN) && request.locked()) {
            throw new IllegalArgumentException("Super admin accounts cannot be locked");
        }

        User updatedUser = userService.setUserLocked(id, request.locked());
        return ApiResponse.success(userMapper.toUserInfoResponse(updatedUser));
    }
}