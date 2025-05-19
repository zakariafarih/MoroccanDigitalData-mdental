package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.patientcore.exception.IdentifierNotFoundException;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.exception.ValidationException;
import org.mdental.patientcore.mapper.IdentifierMapper;
import org.mdental.patientcore.model.dto.IdentifierRequest;
import org.mdental.patientcore.model.dto.IdentifierResponse;
import org.mdental.patientcore.model.entity.IdentifierType;
import org.mdental.patientcore.model.entity.Patient;
import org.mdental.patientcore.model.entity.PatientIdentifier;
import org.mdental.patientcore.repository.IdentifierRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentifierService {

    private final IdentifierRepository identifierRepository;
    private final PatientRepository patientRepository;
    private final IdentifierMapper identifierMapper;

    @Transactional(readOnly = true)
    public List<IdentifierResponse> getPatientIdentifiers(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        return identifierRepository.findByPatientId(patientId).stream()
                .map(identifierMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IdentifierResponse getPatientIdentifierById(UUID clinicId, UUID patientId, UUID identifierId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientIdentifier identifier = identifierRepository.findByPatientIdAndId(patientId, identifierId)
                .orElseThrow(() -> new IdentifierNotFoundException("Identifier not found with ID: " + identifierId));

        return identifierMapper.toDto(identifier);
    }

    @Transactional
    public IdentifierResponse createPatientIdentifier(UUID clinicId, UUID patientId, IdentifierRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);
        validateIdentifier(request);

        PatientIdentifier identifier = identifierMapper.toEntity(request);
        identifier.setPatientId(patientId);

        PatientIdentifier savedIdentifier = identifierRepository.save(identifier);
        return identifierMapper.toDto(savedIdentifier);
    }

    @Transactional
    public IdentifierResponse updatePatientIdentifier(UUID clinicId, UUID patientId, UUID identifierId, IdentifierRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);
        validateIdentifier(request);

        PatientIdentifier existingIdentifier = identifierRepository.findByPatientIdAndId(patientId, identifierId)
                .orElseThrow(() -> new IdentifierNotFoundException("Identifier not found with ID: " + identifierId));

        identifierMapper.updateEntityFromDto(request, existingIdentifier);

        PatientIdentifier updatedIdentifier = identifierRepository.save(existingIdentifier);
        return identifierMapper.toDto(updatedIdentifier);
    }

    @Transactional
    public void deletePatientIdentifier(UUID clinicId, UUID patientId, UUID identifierId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientIdentifier identifier = identifierRepository.findByPatientIdAndId(patientId, identifierId)
                .orElseThrow(() -> new IdentifierNotFoundException("Identifier not found with ID: " + identifierId));

        identifierRepository.delete(identifier);
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }

    private void validateIdentifier(IdentifierRequest request) {
        // Validate identifier format based on type
        if (request.getType() == IdentifierType.NATIONAL_ID &&
                (request.getValue() == null || !request.getValue().matches("^[0-9]{9}$"))) {
            throw new ValidationException("National ID must contain 9 digits");
        }

        // Add more validation rules for other identifier types as needed
    }
}