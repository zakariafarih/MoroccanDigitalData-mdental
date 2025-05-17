package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.BusinessHoursRequest;
import org.mdental.cliniccore.model.dto.BusinessHoursResponse;
import org.mdental.cliniccore.model.entity.BusinessHours;
import org.mdental.cliniccore.service.BusinessHoursService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinics/{clinicId}/hours")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Business Hours", description = "APIs for managing clinic business hours")
public class BusinessHoursController {

    private final BusinessHoursService businessHoursService;
    private final HttpServletRequest request;

    @GetMapping
    @Operation(summary = "Get all business hours for a clinic", description = "Returns all business hours for the specified clinic")
    public ApiResponse<List<BusinessHoursResponse>> getAllBusinessHours(@PathVariable UUID clinicId) {
        log.info("REST request to get all business hours for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        List<BusinessHours> businessHours = businessHoursService.getBusinessHoursByClinicId(clinicId);
        List<BusinessHoursResponse> response = businessHours.stream()
                .map(this::mapToBusinessHoursResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get business hours by ID", description = "Returns the specified business hours")
    public ApiResponse<BusinessHoursResponse> getBusinessHours(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to get business hours: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);
        BusinessHours businessHours = businessHoursService.getBusinessHoursById(id);

        // Validate that the business hours belong to the specified clinic
        if (!businessHours.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Business hours do not belong to the specified clinic");
        }

        return ApiResponse.success(mapToBusinessHoursResponse(businessHours));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create business hours", description = "Creates new business hours for the specified clinic")
    public ApiResponse<BusinessHoursResponse> createBusinessHours(
            @PathVariable UUID clinicId,
            @Valid @RequestBody BusinessHoursRequest request) {
        log.info("REST request to create business hours for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        BusinessHours businessHours = businessHoursService.createBusinessHours(clinicId, request);
        return ApiResponse.success(mapToBusinessHoursResponse(businessHours));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update business hours", description = "Updates existing business hours")
    public ApiResponse<BusinessHoursResponse> updateBusinessHours(
            @PathVariable UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody BusinessHoursRequest request) {
        log.info("REST request to update business hours: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the business hours belong to the specified clinic before updating
        BusinessHours existing = businessHoursService.getBusinessHoursById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Business hours do not belong to the specified clinic");
        }

        BusinessHours businessHours = businessHoursService.updateBusinessHours(id, request);
        return ApiResponse.success(mapToBusinessHoursResponse(businessHours));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete business hours", description = "Deletes the specified business hours")
    public ApiResponse<Void> deleteBusinessHours(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to delete business hours: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the business hours belong to the specified clinic before deleting
        BusinessHours existing = businessHoursService.getBusinessHoursById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Business hours do not belong to the specified clinic");
        }

        businessHoursService.deleteBusinessHours(id);
        return ApiResponse.success(null);
    }

    /**
     * Maps a BusinessHours entity to a BusinessHoursResponse DTO
     */
    private BusinessHoursResponse mapToBusinessHoursResponse(BusinessHours hours) {
        return BusinessHoursResponse.builder()
                .id(hours.getId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .active(hours.getActive())
                .createdAt(hours.getCreatedAt())
                .createdBy(hours.getCreatedBy())
                .updatedAt(hours.getUpdatedAt())
                .updatedBy(hours.getUpdatedBy())
                .build();
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
            throw new ClinicController.AccessDeniedException("You don't have access to this clinic");
        }
    }
}