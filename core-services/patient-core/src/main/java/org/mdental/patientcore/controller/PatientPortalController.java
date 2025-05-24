package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.patientcore.model.dto.PortalLoginRequest;
import org.mdental.patientcore.model.dto.PortalPreferencesRequest;
import org.mdental.patientcore.model.dto.PortalRegisterRequest;
import org.mdental.patientcore.service.PatientPortalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/portal")
@RequiredArgsConstructor
@Tag(name = "Patient Portal", description = "APIs for managing patient portal accounts")
public class PatientPortalController {

    private final PatientPortalService patientPortalService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Register portal account", description = "Registers a new portal account for the patient")
    public ResponseEntity<ApiResponse<Void>> registerPortalAccount(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody PortalRegisterRequest request) {
        patientPortalService.registerPatientPortalAccount(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(null), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','PATIENT')")
    @Operation(summary = "Update login timestamp", description = "Updates the last login timestamp for the patient portal account")
    public ResponseEntity<ApiResponse<Void>> updateLoginTimestamp(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody PortalLoginRequest request) {
        patientPortalService.updatePatientPortalLogin(clinicId, patientId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','PATIENT')")
    @Operation(summary = "Get portal preferences", description = "Gets preferences for the patient portal account")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPortalPreferences(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        Map<String, String> preferences = patientPortalService.getPatientPortalPreferences(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','PATIENT')")
    @Operation(summary = "Update portal preferences", description = "Updates preferences for the patient portal account")
    public ResponseEntity<ApiResponse<Void>> updatePortalPreferences(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody PortalPreferencesRequest request) {
        patientPortalService.updatePatientPortalPreferences(clinicId, patientId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete portal account", description = "Deletes the patient portal account")
    public ResponseEntity<ApiResponse<Void>> deletePortalAccount(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        patientPortalService.deletePatientPortalAccount(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}