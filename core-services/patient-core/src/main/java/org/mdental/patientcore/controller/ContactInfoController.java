package org.mdental.patientcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.model.dto.ContactInfoRequest;
import org.mdental.patientcore.model.dto.ContactInfoResponse;
import org.mdental.patientcore.service.ContactInfoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics/{clinicId}/patients/{patientId}/contacts")
@RequiredArgsConstructor
@Tag(name = "Patient Contact Information", description = "APIs for managing patient contact information")
public class ContactInfoController {

    private final ContactInfoService contactInfoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    public ResponseEntity<ApiResponse<PageResponse<ContactInfoResponse>>> getAllContactInfo(
            @PathVariable UUID clinicId,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        PageResponse<ContactInfoResponse> contacts = contactInfoService.getPatientContactInfo(clinicId, patientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST','DENTIST','HYGIENIST')")
    @Operation(summary = "Get contact information by ID", description = "Returns the contact information with the specified ID")
    public ResponseEntity<ApiResponse<ContactInfoResponse>> getContactInfoById(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {
        ContactInfoResponse contact = contactInfoService.getPatientContactInfoById(clinicId, patientId, contactId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Create new contact information", description = "Creates new contact information for the patient")
    public ResponseEntity<ApiResponse<ContactInfoResponse>> createContactInfo(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody ContactInfoRequest request) {
        ContactInfoResponse contact = contactInfoService.createPatientContactInfo(clinicId, patientId, request);
        return new ResponseEntity<>(ApiResponse.success(contact), HttpStatus.CREATED);
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN','RECEPTIONIST')")
    @Operation(summary = "Update contact information", description = "Updates existing contact information")
    public ResponseEntity<ApiResponse<ContactInfoResponse>> updateContactInfo(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId,
            @Valid @RequestBody ContactInfoRequest request) {
        ContactInfoResponse contact = contactInfoService.updatePatientContactInfo(clinicId, patientId, contactId, request);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CLINIC_ADMIN')")
    @Operation(summary = "Delete contact information", description = "Deletes contact information")
    public ResponseEntity<ApiResponse<Void>> deleteContactInfo(
            @Parameter(description = "Clinic ID", required = true) @PathVariable UUID clinicId,
            @Parameter(description = "Patient ID", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {
        contactInfoService.deletePatientContactInfo(clinicId, patientId, contactId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}