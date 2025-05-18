package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicService {

    private final ClinicQueryService queryService;
    private final ClinicCommandService commandService;

    // Query methods
    public List<Clinic> getAllClinics() {
        return queryService.getAllClinics();
    }

    public Clinic getClinicById(UUID id) {
        return queryService.getClinicById(id);
    }

    public Page<Clinic> getFilteredClinics(String realm, String name, Pageable pageable) {
        return queryService.getFilteredClinics(realm, name, pageable);
    }

    public Clinic getClinicByRealm(String realm) {
        return queryService.getClinicByRealm(realm);
    }

    public Optional<Clinic> findClinicByRealm(String realm) {
        return queryService.findClinicByRealm(realm);
    }

    public Optional<Clinic> findClinicBySlug(String slug) {
        return queryService.findClinicBySlug(slug);
    }

    // Command methods
    public Clinic createClinic(CreateClinicRequest request) {
        return commandService.createClinic(request);
    }

    public Clinic updateClinic(UUID id, UpdateClinicRequest request) {
        return commandService.updateClinic(id, request);
    }

    public Clinic updateClinicStatus(UUID id, Clinic.ClinicStatus status) {
        return commandService.updateClinicStatus(id, status);
    }

    public void deleteClinic(UUID id) {
        commandService.deleteClinic(id);
    }

    public Clinic save(Clinic clinic) {
        return commandService.save(clinic);
    }

    // Custom exceptions
    public static class ClinicNotFoundException extends BaseException {
        public ClinicNotFoundException(String message) {
            super(message, ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    public static class ClinicAlreadyExistsException extends BaseException {
        public ClinicAlreadyExistsException(String message) {
            super(message, ErrorCode.DUPLICATE_RESOURCE);
        }
    }
}