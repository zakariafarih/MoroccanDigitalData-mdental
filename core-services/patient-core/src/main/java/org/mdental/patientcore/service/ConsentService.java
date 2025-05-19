package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.patientcore.exception.ConsentNotFoundException;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.mapper.ConsentMapper;
import org.mdental.patientcore.model.dto.ConsentRequest;
import org.mdental.patientcore.model.dto.ConsentResponse;
import org.mdental.patientcore.model.entity.Consent;
import org.mdental.patientcore.model.entity.ConsentType;
import org.mdental.patientcore.repository.ConsentRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final ConsentMapper consentMapper;

    @Transactional(readOnly = true)
    public List<ConsentResponse> getPatientConsents(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        return consentRepository.findByPatientId(patientId).stream()
                .map(consentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsentResponse getPatientConsentById(UUID clinicId, UUID patientId, UUID consentId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Consent consent = consentRepository.findByPatientIdAndId(patientId, consentId)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found with ID: " + consentId));

        return consentMapper.toDto(consent);
    }

    @Transactional
    public ConsentResponse createOrUpdatePatientConsent(UUID clinicId, UUID patientId, ConsentRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Optional<Consent> existingConsent = consentRepository.findByPatientIdAndType(patientId, request.getType());

        Consent consent;
        if (existingConsent.isPresent()) {
            consent = existingConsent.get();
            consent.setGranted(request.getGranted());
            consent.setGrantedAt(request.getGrantedAt() != null ? request.getGrantedAt() : Instant.now());
            consent.setExpiresAt(request.getExpiresAt());
            consent.setGrantedBy(request.getGrantedBy());
        } else {
            consent = consentMapper.toEntity(request);
            consent.setPatientId(patientId);
            consent.setGrantedAt(request.getGrantedAt() != null ? request.getGrantedAt() : Instant.now());
        }

        Consent savedConsent = consentRepository.save(consent);
        return consentMapper.toDto(savedConsent);
    }

    @Transactional
    public void deletePatientConsent(UUID clinicId, UUID patientId, UUID consentId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        Consent consent = consentRepository.findByPatientIdAndId(patientId, consentId)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found with ID: " + consentId));

        consentRepository.delete(consent);
    }

    @Transactional(readOnly = true)
    public boolean hasConsent(UUID patientId, ConsentType type) {
        Optional<Consent> consent = consentRepository.findByPatientIdAndType(patientId, type);

        return consent.isPresent() &&
                consent.get().isGranted() &&
                (consent.get().getExpiresAt() == null || consent.get().getExpiresAt().isAfter(Instant.now()));
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }
}