package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.exception.ContactInfoNotFoundException;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.exception.ValidationException;
import org.mdental.patientcore.mapper.ContactInfoMapper;
import org.mdental.patientcore.model.dto.ContactInfoRequest;
import org.mdental.patientcore.model.dto.ContactInfoResponse;
import org.mdental.patientcore.model.entity.ContactInfo;
import org.mdental.patientcore.model.entity.ContactType;
import org.mdental.patientcore.repository.ContactInfoRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactInfoService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{8,20}$");

    private final ContactInfoRepository contactInfoRepository;
    private final PatientRepository patientRepository;
    private final ContactInfoMapper contactInfoMapper;

    @Transactional(readOnly = true)
    public List<ContactInfoResponse> getPatientContactInfo(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        return contactInfoRepository.findByPatientId(patientId).stream()
                .map(contactInfoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactInfoResponse> getPatientContactInfo(UUID clinicId, UUID patientId, Pageable pageable) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Page<ContactInfo> contactInfoPage = contactInfoRepository.findByPatientId(patientId, pageable);
        List<ContactInfoResponse> contacts = contactInfoPage.getContent().stream()
                .map(contactInfoMapper::toDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                contacts,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                contactInfoPage.getTotalElements(),
                contactInfoPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ContactInfoResponse getPatientContactInfoById(UUID clinicId, UUID patientId, UUID contactId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        ContactInfo contactInfo = contactInfoRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new ContactInfoNotFoundException("Contact info not found with ID: " + contactId));

        return contactInfoMapper.toDto(contactInfo);
    }

    @Transactional
    public ContactInfoResponse createPatientContactInfo(UUID clinicId, UUID patientId, ContactInfoRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);
        validateContactInfo(request);

        ContactInfo contactInfo = contactInfoMapper.toEntity(request);
        contactInfo.setPatientId(patientId);

        if (request.isPrimary()) {
            resetPrimaryFlag(patientId, request.getType());
        }

        ContactInfo savedContactInfo = contactInfoRepository.save(contactInfo);
        return contactInfoMapper.toDto(savedContactInfo);
    }

    @Transactional
    public ContactInfoResponse updatePatientContactInfo(UUID clinicId, UUID patientId, UUID contactId, ContactInfoRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);
        validateContactInfo(request);

        ContactInfo existingContactInfo = contactInfoRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new ContactInfoNotFoundException("Contact info not found with ID: " + contactId));

        boolean becomingPrimary = !existingContactInfo.isPrimary() && request.isPrimary();

        contactInfoMapper.updateEntityFromDto(request, existingContactInfo);

        if (becomingPrimary) {
            resetPrimaryFlag(patientId, request.getType());
        }

        ContactInfo updatedContactInfo = contactInfoRepository.save(existingContactInfo);
        return contactInfoMapper.toDto(updatedContactInfo);
    }

    @Transactional
    public void deletePatientContactInfo(UUID clinicId, UUID patientId, UUID contactId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        ContactInfo contactInfo = contactInfoRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new ContactInfoNotFoundException("Contact info not found with ID: " + contactId));

        contactInfoRepository.delete(contactInfo);
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }

    private void validateContactInfo(ContactInfoRequest request) {
        String value = request.getValue();

        if (request.getType() == ContactType.EMAIL && !EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Invalid email format");
        }

        if (request.getType() == ContactType.PHONE && !PHONE_PATTERN.matcher(value).matches()) {
            throw new ValidationException("Invalid phone number format");
        }
    }

    private void resetPrimaryFlag(UUID patientId, ContactType type) {
        contactInfoRepository.resetPrimaryFlags(patientId, type);
    }
}