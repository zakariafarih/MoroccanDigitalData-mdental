package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.event.ClinicEvent;
import org.mdental.cliniccore.mapper.ClinicMapper;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.mdental.cliniccore.security.SameClinic;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicMapper clinicMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${clinic.allowed-sort-properties:name,realm,slug,createdAt,status,updatedAt}")
    private String allowedSortPropertiesString;


    // Allowed properties for sorting
    private Set<String> getAllowedSortProperties() {
        return new HashSet<>(Arrays.asList(allowedSortPropertiesString.split(",")));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "clinics")
    public List<Clinic> getAllClinics() {
        log.debug("Fetching all clinics");
        return clinicRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "clinics", key = "#id")
    @SameClinic
    public Clinic getClinicById(UUID id) {
        log.debug("Fetching clinic with ID: {}", id);
        return clinicRepository.findByIdWithAllRelationships(id)
                .orElseThrow(() -> new ClinicNotFoundException("Clinic not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Clinic> getFilteredClinics(String realm, String name, Pageable pageable) {
        log.debug("Fetching filtered clinics with realm: {}, name: {}", realm, name);
        name = name == null ? "" : name;

        // Apply sorting whitelist
        Pageable safePageable = safeSort(pageable);

        if (realm != null) {
            return clinicRepository.findByRealmAndNameContainingIgnoreCase(realm, name, safePageable);
        } else {
            // If no realm filtering needed (system admin), just filter by name
            return clinicRepository.findByNameContainingIgnoreCase(name, safePageable);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "clinics", key = "#realm")
    public Clinic getClinicByRealm(String realm) {
        log.debug("Fetching clinic with realm: {}", realm);
        return clinicRepository.findByRealm(realm)
                .orElseThrow(() -> new ClinicNotFoundException("Clinic not found with realm: " + realm));
    }

    @Transactional(readOnly = true)
    public Optional<Clinic> findClinicByRealm(String realm) {
        log.debug("Finding clinic with realm: {}", realm);
        return clinicRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public Optional<Clinic> findClinicBySlug(String slug) {
        log.debug("Finding clinic with slug: {}", slug);
        return clinicRepository.findBySlug(slug);
    }

    @Transactional
    @CacheEvict(value = "clinics", allEntries = true)
    public Clinic createClinic(CreateClinicRequest request) {
        log.info("Creating new clinic with name: {}", request.getName());

        // Validate that the slug and realm are unique
        clinicRepository.findBySlug(request.getSlug()).ifPresent(c -> {
            throw new ClinicAlreadyExistsException("Clinic with slug " + request.getSlug() + " already exists");
        });

        clinicRepository.findByRealm(request.getRealm()).ifPresent(c -> {
            throw new ClinicAlreadyExistsException("Clinic with realm " + request.getRealm() + " already exists");
        });

        Clinic clinic = clinicMapper.toEntity(request);
        Clinic savedClinic = clinicRepository.save(clinic);

        // Fire event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, savedClinic, ClinicEvent.EventType.CREATED));

        return savedClinic;
    }

    @Transactional
    @CacheEvict(value = "clinics", key = "#id")
    @SameClinic
    public Clinic updateClinic(UUID id, UpdateClinicRequest request) {
        log.info("Updating clinic with ID: {}", id);
        Clinic clinic = getClinicById(id);
        Clinic originalClinic = copyClinic(clinic);

        // Verify slug uniqueness if being updated
        if (request.getSlug() != null && !request.getSlug().equals(clinic.getSlug())) {
            clinicRepository.findBySlug(request.getSlug()).ifPresent(c -> {
                throw new ClinicAlreadyExistsException("Clinic with slug " + request.getSlug() + " already exists");
            });
            clinic.setSlug(request.getSlug());
        }

        // Update fields if they are not null
        if (request.getName() != null) {
            clinic.setName(request.getName());
        }
        if (request.getLegalName() != null) {
            clinic.setLegalName(request.getLegalName());
        }
        if (request.getTaxId() != null) {
            clinic.setTaxId(request.getTaxId());
        }
        if (request.getDescription() != null) {
            clinic.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            clinic.setLogoUrl(request.getLogoUrl());
        }
        if (request.getPrimaryColor() != null) {
            clinic.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            clinic.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getLicenseNumber() != null) {
            clinic.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getLicenseExpiry() != null) {
            clinic.setLicenseExpiry(request.getLicenseExpiry());
        }
        if (request.getPrivacyPolicyUrl() != null) {
            clinic.setPrivacyPolicyUrl(request.getPrivacyPolicyUrl());
        }
        if (request.getDefaultTimeZone() != null) {
            clinic.setDefaultTimeZone(request.getDefaultTimeZone());
        }
        if (request.getDefaultCurrency() != null) {
            clinic.setDefaultCurrency(request.getDefaultCurrency());
        }
        if (request.getLocale() != null) {
            clinic.setLocale(request.getLocale());
        }
        if (request.getStatus() != null) {
            clinic.setStatus(request.getStatus());
        }

        Clinic updatedClinic = clinicRepository.save(clinic);

        // Fire update event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, updatedClinic, ClinicEvent.EventType.UPDATED, originalClinic));

        return updatedClinic;
    }

    @Transactional
    @CacheEvict(value = "clinics", key = "#id")
    @SameClinic
    public Clinic updateClinicStatus(UUID id, Clinic.ClinicStatus status) {
        log.info("Updating clinic status to {} for clinic ID: {}", status, id);
        Clinic clinic = getClinicById(id);
        Clinic originalClinic = copyClinic(clinic);

        clinic.setStatus(status);
        Clinic updatedClinic = clinicRepository.save(clinic);

        // Fire status changed event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, updatedClinic, ClinicEvent.EventType.STATUS_CHANGED, originalClinic));

        return updatedClinic;
    }

    @Transactional
    @CacheEvict(value = "clinics", key = "#id")
    @SameClinic
    public void deleteClinic(UUID id) {
        log.info("Deleting clinic with ID: {}", id);
        Clinic clinic = getClinicById(id);

        // Get current username for soft delete
        String username = getCurrentUsername();
        clinic.softDelete(username);

        clinicRepository.save(clinic);

        // Fire deleted event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, clinic, ClinicEvent.EventType.DELETED));
    }

    /**
     * Creates a copy of the clinic entity for event comparison
     */
    private Clinic copyClinic(Clinic original) {
        return Clinic.builder()
                .id(original.getId())
                .name(original.getName())
                .slug(original.getSlug())
                .realm(original.getRealm())
                .legalName(original.getLegalName())
                .taxId(original.getTaxId())
                .description(original.getDescription())
                .logoUrl(original.getLogoUrl())
                .primaryColor(original.getPrimaryColor())
                .secondaryColor(original.getSecondaryColor())
                .licenseNumber(original.getLicenseNumber())
                .licenseExpiry(original.getLicenseExpiry())
                .privacyPolicyUrl(original.getPrivacyPolicyUrl())
                .defaultTimeZone(original.getDefaultTimeZone())
                .defaultCurrency(original.getDefaultCurrency())
                .locale(original.getLocale())
                .status(original.getStatus())
                .build();
    }

    /**
     * Gets the current authenticated username or "system" if not available
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "system";
    }

    /**
     * Ensures sort properties are in the allowed list
     */
    private Pageable safeSort(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return pageable;
        }

        Set<String> allowedProps = getAllowedSortProperties();
        boolean allAllowed = pageable.getSort().stream()
                .allMatch(order -> allowedProps.contains(order.getProperty()));

        if (allAllowed) {
            return pageable;
        } else {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        }
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

    @Transactional
    public Clinic save(Clinic clinic) {
        return clinicRepository.save(clinic);
    }
}