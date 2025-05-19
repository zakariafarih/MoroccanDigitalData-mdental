package org.mdental.cliniccore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.CreateRealmRequest;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.cliniccore.client.AuthCoreClient;
import org.mdental.cliniccore.event.ClinicEvent;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicProvisioningService {

    private final ClinicService clinicService;
    private final AuthCoreClient authCoreClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${INTERNAL_API_TOKEN:internal-api-token-placeholder}")
    private String internalApiToken;

    @Transactional
    public Clinic provisionClinic(CreateClinicRequest request) {
        log.info("Starting end-to-end clinic provisioning for: {}", request.getName());

        // 1. Create clinic in the database first
        Clinic clinic = clinicService.createClinic(request);

        try {
            // 2. Create realm in Keycloak via auth-core service
            CreateRealmRequest realmRequest = CreateRealmRequest.builder()
                    .realm(request.getRealm())
                    .clinicSlug(request.getSlug())
                    .build();

            ApiResponse<RealmResponse> response = authCoreClient.createRealm(realmRequest);

            if (!response.isSuccess() || response.getData() == null) {
                // If realm creation fails, throw exception to trigger rollback
                throw new RealmProvisioningException("Failed to provision Keycloak realm");
            }

            RealmResponse realmInfo = response.getData();

            // 3. Update clinic with Keycloak realm details
            clinic.setKcIssuer(realmInfo.getIssuer());
            clinic.setKcAdminUser(realmInfo.getKcRealmAdminUser());
            clinic.setKcTmpPassword(realmInfo.getTmpPassword());

            clinic = clinicService.save(clinic);

            // 4. Publish clinic created event for CDC
            applicationEventPublisher.publishEvent(new ClinicEvent(
                    this, clinic, ClinicEvent.EventType.CREATED
            ));

            return clinic;
        } catch (Exception e) {
            log.error("Error during realm provisioning", e);
            // Any exception triggers transaction rollback
            throw new RealmProvisioningException("Clinic creation failed: " + e.getMessage(), e);
        }
    }

    public static class RealmProvisioningException extends BaseException {
        public RealmProvisioningException(String message) {
            super(message, ErrorCode.GENERAL_ERROR);
        }

        public RealmProvisioningException(String message, Throwable cause) {
            super(message, ErrorCode.GENERAL_ERROR, cause);
        }
    }
}