package org.mdental.patientcore.service;

import lombok.RequiredArgsConstructor;
import org.mdental.patientcore.exception.PatientNotFoundException;
import org.mdental.patientcore.exception.PortalAccountAlreadyExistsException;
import org.mdental.patientcore.exception.PortalAccountNotFoundException;
import org.mdental.patientcore.model.dto.PortalLoginRequest;
import org.mdental.patientcore.model.dto.PortalPreferencesRequest;
import org.mdental.patientcore.model.dto.PortalRegisterRequest;
import org.mdental.patientcore.model.entity.PatientPortalAccount;
import org.mdental.patientcore.repository.PatientPortalAccountRepository;
import org.mdental.patientcore.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientPortalService {

    private final PatientPortalAccountRepository portalAccountRepository;
    private final PatientRepository patientRepository;
    // In a real implementation, we would inject a password encoder service
    // private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerPatientPortalAccount(UUID clinicId, UUID patientId, PortalRegisterRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        // Check if patient already has a portal account
        if (portalAccountRepository.findByPatientId(patientId).isPresent()) {
            throw new PortalAccountAlreadyExistsException("Patient already has a portal account");
        }

        // Check if username is already taken
        if (portalAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new PortalAccountAlreadyExistsException("Username already taken");
        }

        PatientPortalAccount account = PatientPortalAccount.builder()
                .patientId(patientId)
                .username(request.getUsername())
                // In a real implementation, we would encode the password
                // .password(passwordEncoder.encode(request.getPassword()))
                .roles(Collections.singleton("ROLE_PATIENT"))
                .preferences(Collections.emptyMap())
                .build();

        portalAccountRepository.save(account);
    }

    @Transactional
    public void updatePatientPortalLogin(UUID clinicId, UUID patientId, PortalLoginRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientPortalAccount account = portalAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PortalAccountNotFoundException("Patient does not have a portal account"));

        // Update last login time
        account.setLastLogin(Instant.now());
        portalAccountRepository.save(account);
    }

    @Transactional
    public Map<String, String> getPatientPortalPreferences(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientPortalAccount account = portalAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PortalAccountNotFoundException("Patient does not have a portal account"));

        return account.getPreferences();
    }

    @Transactional
    public void updatePatientPortalPreferences(UUID clinicId, UUID patientId, PortalPreferencesRequest request) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientPortalAccount account = portalAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PortalAccountNotFoundException("Patient does not have a portal account"));

        account.setPreferences(request.getPreferences());
        portalAccountRepository.save(account);
    }

    @Transactional
    public void deletePatientPortalAccount(UUID clinicId, UUID patientId) {
        verifyPatientBelongsToClinic(patientId, clinicId);

        PatientPortalAccount account = portalAccountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PortalAccountNotFoundException("Patient does not have a portal account"));

        portalAccountRepository.delete(account);
    }

    private void verifyPatientBelongsToClinic(UUID patientId, UUID clinicId) {
        if (!patientRepository.findByIdAndClinicId(patientId, clinicId).isPresent()) {
            throw new PatientNotFoundException("Patient not found with ID: " + patientId);
        }
    }
}