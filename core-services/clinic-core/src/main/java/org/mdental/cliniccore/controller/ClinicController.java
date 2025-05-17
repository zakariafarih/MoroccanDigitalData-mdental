package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.mapper.*;
import org.mdental.cliniccore.model.dto.*;
import org.mdental.cliniccore.model.entity.*;
import org.mdental.cliniccore.service.*;
import org.mdental.commons.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Management", description = "APIs for managing dental clinics")
public class ClinicController {

    private final ClinicService clinicService;
    private final ClinicMapper clinicMapper;
    private final HttpServletRequest request;

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
        verifyClinicAccess(id);
        Clinic clinic = clinicService.getClinicById(id);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update clinic", description = "Updates an existing clinic with the provided details")
    public ApiResponse<ClinicResponse> updateClinic(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        log.info("REST request to update clinic: {}", id);
        verifyClinicAccess(id);
        Clinic clinic = clinicService.updateClinic(id, request);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update clinic status", description = "Updates the status of an existing clinic")
    public ApiResponse<ClinicResponse> updateClinicStatus(
            @PathVariable UUID id,
            @RequestParam Clinic.ClinicStatus status) {
        log.info("REST request to update clinic status: {} to {}", id, status);
        verifyClinicAccess(id);
        Clinic clinic = clinicService.updateClinicStatus(id, status);
        return ApiResponse.success(clinicMapper.toDto(clinic));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete clinic", description = "Deletes an existing clinic and all related data")
    public ApiResponse<Void> deleteClinic(@PathVariable UUID id) {
        log.info("REST request to delete clinic: {}", id);
        verifyClinicAccess(id);
        clinicService.deleteClinic(id);
        return ApiResponse.success(null);
    }

    @GetMapping
    @Operation(summary = "Get all clinics", description = "Returns all clinics with pagination")
    public ApiResponse<Page<ClinicResponse>> getAllClinics(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String[] sort) {

        String realm = null;
        boolean isSuperAdmin = request.isUserInRole("SUPER_ADMIN");

        // If not a super admin, filter by user's clinic realm
        if (!isSuperAdmin) {
            String clinicId = request.getHeader("X-User-ClinicId");
            if (clinicId != null) {
                // Get realm from the clinic ID
                try {
                    Clinic userClinic = clinicService.getClinicById(UUID.fromString(clinicId));
                    realm = userClinic.getRealm();
                } catch (Exception e) {
                    log.warn("Failed to get clinic realm for filtering: {}", e.getMessage());
                }
            }
        }

        org.springframework.data.domain.Pageable pageable = createPageable(page, size, sort);
        Page<Clinic> clinics = clinicService.getFilteredClinics(realm, name, pageable);

        return ApiResponse.success(clinics.map(clinicMapper::toDto));
    }

    /**
     * Verifies the current user has access to the specified clinic
     */
    private void verifyClinicAccess(UUID clinicId) {
        // Super admin has access to all clinics
        if (request.isUserInRole("SUPER_ADMIN")) {
            return;
        }

        // Check if clinic ID in path matches the user's clinic ID from header
        String userClinicId = request.getHeader("X-User-ClinicId");
        if (userClinicId == null || !userClinicId.equals(clinicId.toString())) {
            throw new AccessDeniedException("You don't have access to this clinic");
        }
    }

    // Helper method for pagination
    private org.springframework.data.domain.Pageable createPageable(int page, int size, String[] sort) {
        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "asc";
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}