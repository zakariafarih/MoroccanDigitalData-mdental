package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.patientcore.model.dto.EmergencyContactRequest;
import org.mdental.patientcore.model.dto.EmergencyContactResponse;
import org.mdental.patientcore.service.EmergencyContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/emergency-contacts")
@RequiredArgsConstructor
@Tag(name = "Patient Emergency Contacts", description = "APIs for managing patient emergency contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "List all emergency contacts", description = "Returns all emergency contacts for the specified patient")
    public ResponseEntity<ApiResponse<List<EmergencyContactResponse>>> getAllEmergencyContacts(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId) {
        List<EmergencyContactResponse> contacts = emergencyContactService.getPatientEmergencyContacts(clinicId, patientId);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get emergency contact by ID", description = "Returns the emergency contact with the specified ID")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> getEmergencyContactById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {
        EmergencyContactResponse contact = emergencyContactService.getPatientEmergencyContactById(clinicId, patientId, contactId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create a new emergency contact", description = "Creates a new emergency contact for the patient")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> createEmergencyContact(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody EmergencyContactRequest request) {
        EmergencyContactResponse contact = emergencyContactService.createPatientEmergencyContact(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(contact), HttpStatus.CREATED);
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Update an emergency contact", description = "Updates an existing emergency contact")
    public ResponseEntity<ApiResponse<EmergencyContactResponse>> updateEmergencyContact(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId,
            @Valid @RequestBody EmergencyContactRequest request) {
        EmergencyContactResponse contact = emergencyContactService.updatePatientEmergencyContact(clinicId, patientId, contactId, request);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete an emergency contact", description = "Deletes an emergency contact")
    public ResponseEntity<ApiResponse<Void>> deleteEmergencyContact(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {
        emergencyContactService.deletePatientEmergencyContact(clinicId, patientId, contactId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}