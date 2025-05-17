package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.mapper.*;
import org.mdental.cliniccore.model.dto.*;
import org.mdental.cliniccore.model.entity.*;
import org.mdental.cliniccore.security.TenantGuard;
import org.mdental.cliniccore.service.*;
import org.mdental.commons.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Management", description = "APIs for managing dental clinics")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLINIC_ADMIN')")
public class ClinicController {

    private final ClinicService clinicService;
    private final TenantGuard tenantGuard;

    // Inject all mappers
    private final ClinicMapper clinicMapper;

    @Autowired
    private ClinicProvisioningService clinicProvisioningService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new clinic", description = "Creates a new clinic with the provided details")
    public ApiResponse<ClinicResponse> createClinic(@Valid @RequestBody CreateClinicRequest request) {
        log.info("REST request to create clinic: {}", request.getName());
        Clinic clinic = clinicProvisioningService.provisionClinic(request);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinic by ID", description = "Returns the full clinic profile including related entities")
    public ApiResponse<ClinicResponse> getClinic(@PathVariable UUID id) {
        log.info("REST request to get clinic: {}", id);
        Clinic clinic = clinicService.getClinicById(id);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update clinic", description = "Updates an existing clinic with the provided details")
    public ApiResponse<ClinicResponse> updateClinic(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        log.info("REST request to update clinic: {}", id);
        Clinic clinic = clinicService.updateClinic(id, request);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update clinic status", description = "Updates the status of an existing clinic")
    public ApiResponse<ClinicResponse> updateClinicStatus(
            @PathVariable UUID id,
            @RequestParam Clinic.ClinicStatus status) {
        log.info("REST request to update clinic status: {} to {}", id, status);
        Clinic clinic = clinicService.updateClinicStatus(id, status);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete clinic", description = "Deletes an existing clinic and all related data")
    public ApiResponse<Void> deleteClinic(@PathVariable UUID id) {
        log.info("REST request to delete clinic: {}", id);
        clinicService.deleteClinic(id);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Operation(summary = "Get all clinics", description = "Returns all clinics with pagination")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('CLINIC_ADMIN')")
    public ApiResponse<Page<ClinicResponse>> getAllClinics(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String[] sort) {

        String realm = null;
        // System admins can see all clinics, clinic admins only their own
        if (hasRole("CLINIC_ADMIN") && !hasRole("ROLE_SUPER_ADMIN")) {
            realm = tenantGuard.getCurrentRealm();
        }

        org.springframework.data.domain.Pageable pageable = createPageable(page, size, sort);
        Page<Clinic> clinics = clinicService.getFilteredClinics(realm, name, pageable);

        return ApiResponse.success(clinics.map(clinicMapper::toDto));
    }

    // Helper methods
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private org.springframework.data.domain.Pageable createPageable(int page, int size, String[] sort) {
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}