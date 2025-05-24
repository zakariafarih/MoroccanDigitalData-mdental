package org.mdental.authcore.domain.service;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.event.TenantEvent;
import org.mdental.authcore.domain.model.Tenant;
import org.mdental.authcore.domain.model.User;
import org.mdental.authcore.domain.repository.TenantRepository;
import org.mdental.authcore.exception.DuplicateResourceException;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.exception.ValidationException;
import org.mdental.authcore.util.PasswordGenerator;
import org.mdental.commons.model.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for tenant management operations.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class TenantService {
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9-]+$");

    private final TenantRepository tenantRepository;
    private final UserService userService;
    private final OutboxService outboxService;
    private final PasswordGenerator passwordGenerator;

    /**
     * Create a new tenant.
     *
     * @param slug the tenant slug (unique identifier)
     * @param name the tenant display name
     * @return the created tenant
     */
    @Transactional
    public Tenant createTenant(@NotBlank String slug, @NotBlank String name) {
        log.info("Creating new tenant: {}", slug);

        validateSlug(slug);

        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Tenant with slug '" + slug + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .slug(slug)
                .name(name)
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // Publish tenant created event
        outboxService.saveEvent(
                "Tenant",
                savedTenant.getId(),
                TenantEvent.CREATED.name(),
                null,
                savedTenant
        );

        return savedTenant;
    }

    /**
     * Create a tenant with an admin user.
     *
     * @param slug the tenant slug
     * @param name the tenant name
     * @param adminUsername the admin username
     * @param adminEmail the admin email
     * @param adminFirstName the admin first name
     * @param adminLastName the admin last name
     * @return the created tenant and admin password
     */
    @Transactional
    public TenantWithAdmin createTenantWithAdmin(
            @NotBlank String slug,
            @NotBlank String name,
            @NotBlank String adminUsername,
            @NotBlank String adminEmail,
            @NotBlank String adminFirstName,
            @NotBlank String adminLastName
    ) {
        Tenant tenant = createTenant(slug, name);

        // Generate a strong random password
        String tempPassword = passwordGenerator.generateStrongPassword();

        // Create admin user
        User admin = userService.createUser(
                User.builder()
                        .tenantId(tenant.getId())
                        .username(adminUsername)
                        .email(adminEmail)
                        .firstName(adminFirstName)
                        .lastName(adminLastName)
                        .emailVerified(true) // Admin email is pre-verified
                        .roles(Set.of(Role.CLINIC_ADMIN))
                        .build(),
                tempPassword
        );

        log.info("Created admin user {} for tenant {}", admin.getUsername(), tenant.getSlug());

        return new TenantWithAdmin(tenant, tempPassword);
    }

    /**
     * Get a tenant by ID.
     *
     * @param tenantId the tenant ID
     * @return the tenant
     */
    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found with ID: " + tenantId));
    }

    /**
     * Get a tenant by slug.
     *
     * @param slug the tenant slug
     * @return the tenant
     */
    @Transactional(readOnly = true)
    public Tenant getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Tenant not found with slug: " + slug));
    }

    /**
     * Update a tenant.
     *
     * @param tenantId the tenant ID
     * @param name the new name (or null to keep current)
     * @param active the new active status (or null to keep current)
     * @return the updated tenant
     */
    @Transactional
    public Tenant updateTenant(UUID tenantId, String name, Boolean active) {
        Tenant tenant = getTenantById(tenantId);

        boolean changed = false;

        if (name != null && !name.equals(tenant.getName())) {
            tenant.setName(name);
            changed = true;
        }

        if (active != null && active != tenant.isActive()) {
            tenant.setActive(active);
            changed = true;
        }

        if (changed) {
            tenant = tenantRepository.save(tenant);

            // Publish tenant updated event
            outboxService.saveEvent(
                    "Tenant",
                    tenant.getId(),
                    TenantEvent.UPDATED.name(),
                    null,
                    tenant
            );
        }

        return tenant;
    }

    /**
     * Validate a tenant slug.
     *
     * @param slug the slug to validate
     * @throws ValidationException if the slug is invalid
     */
    private void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Tenant slug cannot be empty");
        }

        if (slug.length() < 3 || slug.length() > 50) {
            throw new ValidationException("Tenant slug must be between 3 and 50 characters");
        }

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new ValidationException("Tenant slug must contain only lowercase letters, numbers, and hyphens");
        }
    }

    /**
     * Container for tenant and admin password.
     */
    public record TenantWithAdmin(Tenant tenant, String adminPassword) {}
}