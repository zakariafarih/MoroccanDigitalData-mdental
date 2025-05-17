package org.mdental.cliniccore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.ContactInfoRequest;
import org.mdental.cliniccore.model.dto.ContactInfoResponse;
import org.mdental.cliniccore.model.entity.ContactInfo;
import org.mdental.cliniccore.service.ContactInfoService;
import org.mdental.commons.model.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clinics/{clinicId}/contacts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clinic Contact Information", description = "APIs for managing clinic contact information")
public class ContactInfoController {

    private final ContactInfoService contactInfoService;
    private final HttpServletRequest request;

    @GetMapping
    @Operation(summary = "Get all contacts for a clinic", description = "Returns all contact information for the specified clinic")
    public ApiResponse<List<ContactInfoResponse>> getAllContactInfo(@PathVariable UUID clinicId) {
        log.info("REST request to get all contact info for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        List<ContactInfo> contactInfos = contactInfoService.getContactInfoByClinicId(clinicId);
        List<ContactInfoResponse> response = contactInfos.stream()
                .map(this::mapToContactInfoResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contact by ID", description = "Returns the specified contact information")
    public ApiResponse<ContactInfoResponse> getContactInfo(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to get contact info: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);
        ContactInfo contactInfo = contactInfoService.getContactInfoById(id);

        // Validate that the contact belongs to the specified clinic
        if (!contactInfo.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Contact info does not belong to the specified clinic");
        }

        return ApiResponse.success(mapToContactInfoResponse(contactInfo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create contact", description = "Creates new contact information for the specified clinic")
    public ApiResponse<ContactInfoResponse> createContactInfo(
            @PathVariable UUID clinicId,
            @Valid @RequestBody ContactInfoRequest request) {
        log.info("REST request to create contact info for clinic: {}", clinicId);
        verifyClinicAccess(clinicId);
        ContactInfo contactInfo = contactInfoService.createContactInfo(clinicId, request);
        return ApiResponse.success(mapToContactInfoResponse(contactInfo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update contact", description = "Updates existing contact information")
    public ApiResponse<ContactInfoResponse> updateContactInfo(
            @PathVariable UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody ContactInfoRequest request) {
        log.info("REST request to update contact info: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the contact belongs to the specified clinic before updating
        ContactInfo existing = contactInfoService.getContactInfoById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Contact info does not belong to the specified clinic");
        }

        ContactInfo contactInfo = contactInfoService.updateContactInfo(id, request);
        return ApiResponse.success(mapToContactInfoResponse(contactInfo));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete contact", description = "Deletes the specified contact information")
    public ApiResponse<Void> deleteContactInfo(
            @PathVariable UUID clinicId,
            @PathVariable UUID id) {
        log.info("REST request to delete contact info: {} for clinic: {}", id, clinicId);
        verifyClinicAccess(clinicId);

        // Verify the contact belongs to the specified clinic before deleting
        ContactInfo existing = contactInfoService.getContactInfoById(id);
        if (!existing.getClinic().getId().equals(clinicId)) {
            throw new IllegalArgumentException("Contact info does not belong to the specified clinic");
        }

        contactInfoService.deleteContactInfo(id);
        return ApiResponse.success(null);
    }

    /**
     * Maps a ContactInfo entity to a ContactInfoResponse DTO
     */
    private ContactInfoResponse mapToContactInfoResponse(ContactInfo contactInfo) {
        return ContactInfoResponse.builder()
                .id(contactInfo.getId())
                .type(contactInfo.getType())
                .label(contactInfo.getLabel())
                .value(contactInfo.getValue())
                .primary(contactInfo.getPrimary())
                .createdAt(contactInfo.getCreatedAt())
                .createdBy(contactInfo.getCreatedBy())
                .updatedAt(contactInfo.getUpdatedAt())
                .updatedBy(contactInfo.getUpdatedBy())
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