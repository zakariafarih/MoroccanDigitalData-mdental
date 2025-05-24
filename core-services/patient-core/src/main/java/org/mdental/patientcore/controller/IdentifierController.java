package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.patientcore.model.dto.IdentifierRequest;
import org.mdental.patientcore.model.dto.IdentifierResponse;
import org.mdental.patientcore.service.IdentifierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/identifiers")
@RequiredArgsConstructor
@Tag(name = "Patient Identifiers", description = "APIs for managing patient identifiers")
public class IdentifierController {

    private final IdentifierService identifierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "List all identifiers", description = "Returns all identifiers for the specified patient")
    public ResponseEntity<ApiResponse<List<IdentifierResponse>>> getAllIdentifiers(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        List<IdentifierResponse> identifiers = identifierService.getPatientIdentifiers(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(identifiers));
    }

    @GetMapping("/{identifierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get identifier by ID", description = "Returns the identifier with the specified ID")
    public ResponseEntity<ApiResponse<IdentifierResponse>> getIdentifierById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Identifier ID", required = true) @PathVariable UUID identifierId) {
        IdentifierResponse identifier = identifierService.getPatientIdentifierById(clinicId, patientId, identifierId);
        return ResponseEntity.ok(ApiResponse.success(identifier));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create a new identifier", description = "Creates a new identifier for the patient")
    public ResponseEntity<ApiResponse<IdentifierResponse>> createIdentifier(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody IdentifierRequest request) {
        IdentifierResponse identifier = identifierService.createPatientIdentifier(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(identifier), HttpStatus.CREATED);
    }

    @PutMapping("/{identifierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Update an identifier", description = "Updates an existing identifier")
    public ResponseEntity<ApiResponse<IdentifierResponse>> updateIdentifier(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Identifier ID", required = true) @PathVariable UUID identifierId,
            @Valid @RequestBody IdentifierRequest request) {
        IdentifierResponse identifier = identifierService.updatePatientIdentifier(clinicId, patientId, identifierId, request);
        return ResponseEntity.ok(ApiResponse.success(identifier));
    }

    @DeleteMapping("/{identifierId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete an identifier", description = "Deletes an identifier")
    public ResponseEntity<ApiResponse<Void>> deleteIdentifier(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Identifier ID", required = true) @PathVariable UUID identifierId) {
        identifierService.deletePatientIdentifier(clinicId, patientId, identifierId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}