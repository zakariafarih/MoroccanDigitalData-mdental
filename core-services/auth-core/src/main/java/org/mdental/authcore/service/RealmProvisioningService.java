package org.mdental.authcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.authcore.client.KeycloakClient;
import org.mdental.authcore.exception.ValidationException;
import org.mdental.authcore.model.entity.Realm;
import org.mdental.authcore.repository.RealmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealmProvisioningService {

    private final KeycloakClient keycloakClient;
    private final RealmRepository realmRepository;
    private final OutboxService outboxService;

    @Transactional
    public RealmResponse createRealm(String realmName, String clinicSlug) {
        log.info("Provisioning realm: {} for clinic: {}", realmName, clinicSlug);

        // Validate realm name format
        validateRealmName(realmName);

        // Validate clinicSlug format
        validateClinicSlug(clinicSlug);

        // Create realm in Keycloak
        Map<String, String> keycloakResponse = keycloakClient.createRealm(realmName, clinicSlug);

        // Create realm entity in database
        Realm realm = Realm.builder()
                .name(realmName)
                .clinicSlug(clinicSlug)
                .issuer(keycloakResponse.get("issuer"))
                .adminUsername(keycloakResponse.get("kcRealmAdminUser"))
                .createdAt(Instant.now())
                .build();

        Realm savedRealm = realmRepository.save(realm);

        // Publish event via outbox
        outboxService.saveEvent(
                "Realm",
                savedRealm.getId(),
                "CREATED",
                null,
                savedRealm);

        // Return response
        return RealmResponse.builder()
                .issuer(keycloakResponse.get("issuer"))
                .realmName(realmName)
                .kcRealmAdminUser(keycloakResponse.get("kcRealmAdminUser"))
                .tmpPassword(keycloakResponse.get("tmpPassword"))
                .build();
    }

    @Transactional
    public RealmResponse regenerateAdminPassword(String realmName, String adminUsername) {
        log.info("Regenerating admin password for realm: {}", realmName);

        // Validate realm name
        validateRealmName(realmName);

        // Find realm in database
        Realm realm = realmRepository.findByName(realmName)
                .orElseThrow(() -> new ValidationException("Realm not found: " + realmName));

        // Call Keycloak to reset password
        Map<String, String> keycloakResponse = keycloakClient.regenerateAdminPassword(realmName, adminUsername);

        // Update realm in database if admin username changed
        if (!adminUsername.equals(realm.getAdminUsername())) {
            realm.setAdminUsername(adminUsername);
            realm.setUpdatedAt(Instant.now());
            realmRepository.save(realm);
        }

        // Publish event via outbox
        outboxService.saveEvent(
                "Realm",
                realm.getId(),
                "PASSWORD_ROTATED",
                null,
                Map.of("realmName", realmName, "adminUsername", adminUsername));

        // Return response
        return RealmResponse.builder()
                .issuer(keycloakResponse.get("issuer"))
                .realmName(realmName)
                .kcRealmAdminUser(adminUsername)
                .tmpPassword(keycloakResponse.get("tmpPassword"))
                .build();
    }

    private void validateRealmName(String realmName) {
        if (realmName == null || !realmName.matches("^mdental-[a-z0-9-]+$")) {
            throw new ValidationException(
                    "Realm name must start with 'mdental-' followed by lowercase letters, numbers, and hyphens");
        }
    }

    private void validateClinicSlug(String clinicSlug) {
        if (clinicSlug == null || !clinicSlug.matches("^[a-z0-9-]+$")) {
            throw new ValidationException(
                    "Clinic slug must contain only lowercase letters, numbers, and hyphens");
        }
    }
}