package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.commons.model.PageResponse;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.exception.ValidationException;
import org.mdental.patientcore.mapper.PatientMapper;
import org.mdental.patientcore.model.dto.PatientRequest;
import org.mdental.patientcore.model.dto.PatientResponse;
import org.mdental.patientcore.model.dto.PatientSummaryResponse;
import org.mdental.patientcore.model.entity.Patient;
import org.mdental.patientcore.model.entity.PatientStatus;
import org.mdental.patientcore.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final IdentifierRepository identifierRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final AddressRepository addressRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final ConsentRepository consentRepository;
    private final PatientPortalAccountRepository portalAccountRepository;

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> listAllPatients(UUID clinicId, Pageable pageable) {
        Page<Patient> patientPage = patientRepository.findByClinicId(clinicId, pageable);
        List<PatientResponse> patients = patientPage.getContent().stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                patients,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                patientPage.getTotalElements(),
                patientPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listPatientsByStatus(UUID clinicId, PatientStatus status) {
        return patientRepository.findByClinicIdAndStatus(clinicId, status).stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientResponse> searchPatients(UUID clinicId, String searchTerm, Pageable pageable) {
        Page<Patient> page = patientRepository.findByClinicIdAndNameContainingIgnoreCase(clinicId, searchTerm, pageable);
        List<PatientResponse> dtos = page.getContent().stream()
                .map(patientMapper::toDto)
                .collect(Collectors.toList());
        return new PageResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID clinicId, UUID patientId) {
        Patient patient = findPatientByIdAndClinicId(patientId, clinicId);
        return patientMapper.toDto(patient);
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientSummaryResponse> getPatientSummaries(UUID clinicId, Pageable pageable) {
        Page<Patient> patientPage = patientRepository.findByClinicId(clinicId, pageable);
        List<PatientSummaryResponse> summaries = patientPage.getContent().stream()
                .map(patientMapper::toSummaryDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                summaries,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                patientPage.getTotalElements(),
                patientPage.getTotalPages()
        );
    }

    @Transactional
    public PatientResponse createPatient(UUID clinicId, PatientRequest request) {
        validatePatient(request);

        Patient patient = patientMapper.toEntity(request);
        patient.setClinicId(clinicId);

        Patient savedPatient = patientRepository.save(patient);
        return patientMapper.toDto(savedPatient);
    }

    @Transactional
    public PatientResponse updatePatient(UUID clinicId, UUID patientId, PatientRequest request) {
        validatePatient(request);

        Patient existingPatient = findPatientByIdAndClinicId(patientId, clinicId);
        patientMapper.updateEntityFromDto(request, existingPatient);

        Patient updatedPatient = patientRepository.save(existingPatient);
        return patientMapper.toDto(updatedPatient);
    }

    @Transactional
    public void deletePatient(UUID clinicId, UUID patientId) {
        Patient patient = findPatientByIdAndClinicId(patientId, clinicId);
        patientRepository.delete(patient);
    }

    @Transactional
    public void purgePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));

        // Cascade delete all related entities in a specific order to avoid foreign key constraints
        identifierRepository.findByPatientId(patientId)
                .forEach(identifierRepository::delete);

        contactInfoRepository.findByPatientId(patientId)
                .forEach(contactInfoRepository::delete);

        addressRepository.findByPatientId(patientId)
                .forEach(addressRepository::delete);

        emergencyContactRepository.findByPatientId(patientId)
                .forEach(emergencyContactRepository::delete);

        consentRepository.findByPatientId(patientId)
                .forEach(consentRepository::delete);

        portalAccountRepository.findByPatientId(patientId)
                .ifPresent(portalAccountRepository::delete);

        // Finally delete the patient
        patientRepository.delete(patient);
    }

    private Patient findPatientByIdAndClinicId(UUID patientId, UUID clinicId) {
        return patientRepository.findByIdAndClinicId(patientId, clinicId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + patientId));
    }

    private void validatePatient(PatientRequest request) {
        // Validate age (e.g., patient should not be over 150 years old)
        LocalDate dob = request.getDateOfBirth();
        if (dob != null) {
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age > 150) {
                throw new ValidationException("Patient age exceeds 150 years");
            }
            if (age < 0) {
                throw new ValidationException("Date of birth cannot be in the future");
            }
        }
    }
}