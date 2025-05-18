package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.event.ClinicEvent;
import org.mdental.cliniccore.mapper.ClinicMapper;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.dto.UpdateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicCommandService {

    private final ClinicRepository clinicRepository;
    private final ClinicQueryService clinicQueryService;
    private final ClinicMapper clinicMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    @CacheEvict(value = "clinics", allEntries = true)
    public Clinic createClinic(CreateClinicRequest request) {
        log.info("Creating new clinic with name: {}", request.getName());

        // Validate that the slug and realm are unique
        clinicQueryService.findClinicBySlug(request.getSlug()).ifPresent(c -> {
            throw new ClinicService.ClinicAlreadyExistsException("Clinic with slug " + request.getSlug() + " already exists");
        });

        clinicQueryService.findClinicByRealm(request.getRealm()).ifPresent(c -> {
            throw new ClinicService.ClinicAlreadyExistsException("Clinic with realm " + request.getRealm() + " already exists");
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
    public Clinic updateClinic(UUID id, UpdateClinicRequest request) {
        log.info("Updating clinic with ID: {}", id);
        Clinic clinic = clinicQueryService.getClinicById(id);
        Clinic originalClinic = copyClinic(clinic);

        // Verify slug uniqueness if being updated
        if (request.getSlug() != null && !request.getSlug().equals(clinic.getSlug())) {
            clinicQueryService.findClinicBySlug(request.getSlug()).ifPresent(c -> {
                throw new ClinicService.ClinicAlreadyExistsException("Clinic with slug " + request.getSlug() + " already exists");
            });
            clinic.setSlug(request.getSlug());
        }

        // Update fields if they are not null
        updateClinicFields(clinic, request);

        Clinic updatedClinic = clinicRepository.save(clinic);

        // Fire update event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, updatedClinic, ClinicEvent.EventType.UPDATED, originalClinic));

        return updatedClinic;
    }

    private void updateClinicFields(Clinic clinic, UpdateClinicRequest request) {
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
    }

    @Transactional
    @CacheEvict(value = "clinics", key = "#id")
    public Clinic updateClinicStatus(UUID id, Clinic.ClinicStatus status) {
        log.info("Updating clinic status to {} for clinic ID: {}", status, id);
        Clinic clinic = clinicQueryService.getClinicById(id);
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
    public void deleteClinic(UUID id) {
        log.info("Deleting clinic with ID: {}", id);
        Clinic clinic = clinicQueryService.getClinicById(id);

        // Get current username for soft delete
        String username = getCurrentUsername();
        clinic.softDelete(username);

        clinicRepository.save(clinic);

        // Fire deleted event
        applicationEventPublisher.publishEvent(
                new ClinicEvent(this, clinic, ClinicEvent.EventType.DELETED));
    }

    @Transactional
    public Clinic save(Clinic clinic) {
        return clinicRepository.save(clinic);
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
}