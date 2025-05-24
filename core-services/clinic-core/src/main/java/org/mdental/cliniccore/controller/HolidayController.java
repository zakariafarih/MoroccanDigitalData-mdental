package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.HolidayRequest;
import org.mdental.cliniccore.model.dto.HolidayResponse;
import org.mdental.cliniccore.model.entity.Holiday;
import org.mdental.cliniccore.service.HolidayService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinics/{clinicId}/holidays")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Holidays", description = "APIs for managing clinic holidays")
public class HolidayController {

    private final HolidayService holidayService;
    private final HttpServletRequest request;

    @GetMapping
    @Operation(summary = "Get all holidays for a clinic", description = "Returns all holidays for the specified clinic")
    public ApiResponse<List<HolidayResponse>> getAllHolidays(@PathVariable UUID clinicId) {
        log.info("REST request to get all holidays for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        List<Holiday> holidays = holidayService.getHolidaysByClinicId(clinicId);
        List<HolidayResponse> response = holidays.stream()
                .map(this::mapToHolidayResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/range")
    @Operation(summary = "Get holidays for a date range", description = "Returns holidays for the specified clinic within a date range")
    public ApiResponse<List<HolidayResponse>> getHolidaysInDateRange(
            @PathVariable UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get holidays for clinic: {} between {} and {}", clinicId, startDate, endDate);
        verifyClinicAccess(clinicId);
        List<Holiday> holidays = holidayService.getHolidaysByClinicIdAndDateRange(clinicId, startDate, endDate);
        List<HolidayResponse> response = holidays.stream()
                .map(this::mapToHolidayResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get holiday by ID", description = "Returns the specified holiday")
    public ApiResponse<HolidayResponse> getHoliday(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to get holiday: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);
        Holiday holiday = holidayService.getHolidayById(id);

        // Validate that the holiday belongs to the specified clinic
        if (!holiday.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Holiday does not belong to the specified clinic");
        }

        return ApiResponse.success(mapToHolidayResponse(holiday));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create holiday", description = "Creates a new holiday for the specified clinic")
    public ApiResponse<HolidayResponse> createHoliday(
            @PathVariable UUID clinicId,
            @Valid @RequestBody HolidayRequest request) {
        log.info("REST request to create holiday for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        Holiday holiday = holidayService.createHoliday(clinicId, request);
        return ApiResponse.success(mapToHolidayResponse(holiday));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update holiday", description = "Updates an existing holiday")
    public ApiResponse<HolidayResponse> updateHoliday(
            @PathVariable UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody HolidayRequest request) {
        log.info("REST request to update holiday: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the holiday belongs to the specified clinic before updating
        Holiday existing = holidayService.getHolidayById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Holiday does not belong to the specified clinic");
        }

        Holiday holiday = holidayService.updateHoliday(id, request);
        return ApiResponse.success(mapToHolidayResponse(holiday));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete holiday", description = "Deletes the specified holiday")
    public ApiResponse<Void> deleteHoliday(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to delete holiday: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the holiday belongs to the specified clinic before deleting
        Holiday existing = holidayService.getHolidayById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Holiday does not belong to the specified clinic");
        }

        holidayService.deleteHoliday(id);
        return ApiResponse.success(null);
    }

    /**
     * Maps a Holiday entity to a HolidayResponse DTO
     */
    private HolidayResponse mapToHolidayResponse(Holiday holiday) {
        return HolidayResponse.builder()
                .id(holiday.getId())
                .date(holiday.getDate())
                .description(holiday.getDescription())
                .recurring(holiday.getRecurring())
                .createdAt(holiday.getCreatedAt())
                .createdBy(holiday.getCreatedBy())
                .updatedAt(holiday.getUpdatedAt())
                .updatedBy(holiday.getUpdatedBy())
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