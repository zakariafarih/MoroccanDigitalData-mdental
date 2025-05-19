package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.patientcore.exception.EmergencyContactNotFoundException;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.mapper.EmergencyContactMapper;
import org.mdental.patientcore.model.dto.EmergencyContactRequest;
import org.mdental.patientcore.model.dto.EmergencyContactResponse;
import org.mdental.patientcore.model.entity.EmergencyContact;
import org.mdental.patientcore.repository.EmergencyContactRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final PatientRepository patientRepository;
    private final EmergencyContactMapper emergencyContactMapper;

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> getPatientEmergencyContacts(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        return emergencyContactRepository.findByPatientId(patientId).stream()
                .map(emergencyContactMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmergencyContactResponse getPatientEmergencyContactById(UUID clinicId, UUID patientId, UUID contactId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        EmergencyContact emergencyContact = emergencyContactRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new EmergencyContactNotFoundException("Emergency contact not found with ID: " + contactId));

        return emergencyContactMapper.toDto(emergencyContact);
    }

    @Transactional
    public EmergencyContactResponse createPatientEmergencyContact(UUID clinicId, UUID patientId, EmergencyContactRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        EmergencyContact emergencyContact = emergencyContactMapper.toEntity(request);
        emergencyContact.setPatientId(patientId);

        if (request.isPrimary()) {
            resetPrimaryFlag(patientId);
        }

        EmergencyContact savedEmergencyContact = emergencyContactRepository.save(emergencyContact);
        return emergencyContactMapper.toDto(savedEmergencyContact);
    }

    @Transactional
    public EmergencyContactResponse updatePatientEmergencyContact(UUID clinicId, UUID patientId, UUID contactId, EmergencyContactRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        EmergencyContact existingEmergencyContact = emergencyContactRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new EmergencyContactNotFoundException("Emergency contact not found with ID: " + contactId));

        boolean becomingPrimary = !existingEmergencyContact.isPrimary() && request.isPrimary();

        emergencyContactMapper.updateEntityFromDto(request, existingEmergencyContact);

        if (becomingPrimary) {
            resetPrimaryFlag(patientId);
        }

        EmergencyContact updatedEmergencyContact = emergencyContactRepository.save(existingEmergencyContact);
        return emergencyContactMapper.toDto(updatedEmergencyContact);
    }

    @Transactional
    public void deletePatientEmergencyContact(UUID clinicId, UUID patientId, UUID contactId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        EmergencyContact emergencyContact = emergencyContactRepository.findByPatientIdAndId(patientId, contactId)
                .orElseThrow(() -> new EmergencyContactNotFoundException("Emergency contact not found with ID: " + contactId));

        emergencyContactRepository.delete(emergencyContact);
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }

    private void resetPrimaryFlag(UUID patientId) {
        emergencyContactRepository.resetPrimaryFlags(patientId);
    }
}