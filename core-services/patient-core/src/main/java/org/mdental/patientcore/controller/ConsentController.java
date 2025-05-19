package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.patientcore.model.dto.ConsentRequest;
import org.mdental.patientcore.model.dto.ConsentResponse;
import org.mdental.patientcore.model.entity.ConsentType;
import org.mdental.patientcore.service.ConsentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/consents")
@RequiredArgsConstructor
@Tag(name = "Patient Consents", description = "APIs for managing patient consents")
public class ConsentController {

    private final ConsentService consentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "List all consents", description = "Returns all consents for the specified patient")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getAllConsents(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        List<ConsentResponse> consents = consentService.getPatientConsents(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(consents));
    }

    @GetMapping("/{consentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get consent by ID", description = "Returns the consent with the specified ID")
    public ResponseEntity<ApiResponse<ConsentResponse>> getConsentById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Consent ID", required = true) @PathVariable UUID consentId) {
        ConsentResponse consent = consentService.getPatientConsentById(clinicId, patientId, consentId);
        return ResponseEntity.ok(ApiResponse.success(consent));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create or update a consent", description = "Creates or updates a consent for the patient")
    public ResponseEntity<ApiResponse<ConsentResponse>> createOrUpdateConsent(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody ConsentRequest request) {
        ConsentResponse consent = consentService.createOrUpdatePatientConsent(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(consent), HttpStatus.CREATED);
    }

    @DeleteMapping("/{consentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete a consent", description = "Deletes a consent")
    public ResponseEntity<ApiResponse<Void>> deleteConsent(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Consent ID", required = true) @PathVariable UUID consentId) {
        consentService.deletePatientConsent(clinicId, patientId, consentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/verify/{type}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Verify consent", description = "Verifies if the patient has granted a specific consent")
    public ResponseEntity<ApiResponse<Boolean>> verifyConsent(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Consent type", required = true) @PathVariable ConsentType type) {
        boolean hasConsent = consentService.hasConsent(patientId, type);
        return ResponseEntity.ok(ApiResponse.success(hasConsent));
    }
}