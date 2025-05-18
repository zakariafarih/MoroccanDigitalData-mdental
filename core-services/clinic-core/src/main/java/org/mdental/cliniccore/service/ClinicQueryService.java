package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
public class ClinicQueryService {

    private final ClinicRepository clinicRepository;

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
    public Clinic getClinicById(UUID id) {
        log.debug("Fetching clinic with ID: {}", id);
        return clinicRepository.findByIdWithAllRelationships(id)
                .orElseThrow(() -> new ClinicService.ClinicNotFoundException("Clinic not found with ID: " + id));
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
                .orElseThrow(() -> new ClinicService.ClinicNotFoundException("Clinic not found with realm: " + realm));
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
}