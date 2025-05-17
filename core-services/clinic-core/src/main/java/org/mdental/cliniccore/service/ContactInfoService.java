package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.mapper.ContactInfoMapper;
import org.mdental.cliniccore.model.dto.ContactInfoRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.model.entity.ContactInfo;
import org.mdental.cliniccore.repository.ContactInfoRepository;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactInfoService {

    private final ContactInfoRepository contactInfoRepository;
    private final ClinicService clinicService;
    private final ContactInfoMapper contactInfoMapper;

    @Transactional(readOnly = true)
    public List<ContactInfo> getContactInfoByClinicId(UUID clinicId) {
        log.debug("Fetching contact info for clinic ID: {}", clinicId);
        // Verify clinic exists
        clinicService.getClinicById(clinicId);
        return contactInfoRepository.findByClinicId(clinicId);
    }

    @Transactional(readOnly = true)
    public ContactInfo getContactInfoById(UUID id) {
        log.debug("Fetching contact info with ID: {}", id);
        return contactInfoRepository.findById(id)
                .orElseThrow(() -> new ContactInfoNotFoundException("Contact info not found with ID: " + id));
    }

    @Transactional
    public ContactInfo createContactInfo(UUID clinicId, ContactInfoRequest request) {
        log.info("Creating new contact info for clinic ID: {}", clinicId);
        request.validate();

        Clinic clinic = clinicService.getClinicById(clinicId);

        // If this is a primary contact, update existing primary contacts of the same type
        if (Boolean.TRUE.equals(request.getPrimary())) {
            handlePrimaryContactInfo(clinicId, request.getType());
        } else {
            // If this is not primary, ensure there's at least one primary contact of this type
            ensureOnePrimaryContact(clinicId, request.getType());
        }

        ContactInfo contactInfo = contactInfoMapper.toEntity(request);
        contactInfo.setClinic(clinic);

        return contactInfoRepository.save(contactInfo);
    }

    private void ensureOnePrimaryContact(UUID clinicId, ContactInfo.ContactType type) {
        List<ContactInfo> contacts = contactInfoRepository.findByClinicIdAndType(clinicId, type);
        long primaryCount = contacts.stream()
                .filter(ContactInfo::getPrimary)
                .count();

        if (primaryCount == 0 && !contacts.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one " + type + " contact must be marked as primary");
        }
    }

    @Transactional
    public ContactInfo updateContactInfo(UUID id, ContactInfoRequest request) {
        log.info("Updating contact info with ID: {}", id);
        request.validate(); // Validate request

        ContactInfo contactInfo = getContactInfoById(id);

        // If this is becoming primary, handle existing primary contacts
        if (Boolean.TRUE.equals(request.getPrimary()) && !Boolean.TRUE.equals(contactInfo.getPrimary())) {
            handlePrimaryContactInfo(contactInfo.getClinic().getId(), request.getType());
        }

        contactInfo.setType(request.getType());
        contactInfo.setLabel(request.getLabel());
        contactInfo.setValue(request.getValue());
        contactInfo.setPrimary(request.getPrimary());

        return contactInfoRepository.save(contactInfo);
    }

    @Transactional
    public void deleteContactInfo(UUID id) {
        log.info("Deleting contact info with ID: {}", id);
        ContactInfo contactInfo = getContactInfoById(id);

        // Use soft delete instead of hard delete
        String username = getCurrentUsername();
        contactInfo.softDelete(username);
        contactInfoRepository.save(contactInfo);
    }

    /**
     * Helper method to handle primary contact info - ensures only one contact of each type is primary
     */
    private void handlePrimaryContactInfo(UUID clinicId, ContactInfo.ContactType type) {
        Optional<ContactInfo> existingPrimary = contactInfoRepository.findPrimaryContactByType(clinicId, type);
        existingPrimary.ifPresent(contact -> {
            contact.setPrimary(false);
            contactInfoRepository.save(contact);
        });
    }

    /**
     * Gets the current username from request header or "system" if not available
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String username = request.getHeader("X-User-Username");
                if (username != null && !username.isBlank()) {
                    return username;
                }
            }
        } catch (Exception e) {
            // Fall back to system user
        }

        return "system";
    }

    // Custom exceptions
    public static class ContactInfoNotFoundException extends BaseException {
        public ContactInfoNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}