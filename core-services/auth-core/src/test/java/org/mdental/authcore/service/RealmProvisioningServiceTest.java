package org.mdental.authcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.authcore.api.dto.RealmResponse;
import org.mdental.authcore.client.KeycloakClient;
import org.mdental.authcore.exception.ValidationException;
import org.mdental.authcore.model.entity.Realm;
import org.mdental.authcore.repository.RealmRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealmProvisioningServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private RealmRepository realmRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PasswordGenerator passwordGenerator;

    private RealmProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new RealmProvisioningService(keycloakClient, realmRepository, outboxService, passwordGenerator);
    }

    @Test
    void createRealm_Success() {
        // Arrange
        String realmName = "mdental-test";
        String clinicSlug = "test";

        Map<String, String> keycloakResponse = Map.of(
                "issuer", "http://localhost:8080/realms/mdental-test",
                "kcRealmAdminUser", "admin",
                "tmpPassword", "randomPassword123",
                "serviceClientId", "mdental-frontend",
                "serviceClientSecret", "clientSecret123"
        );

        when(keycloakClient.createRealm(realmName, clinicSlug)).thenReturn(keycloakResponse);

        Realm savedRealm = Realm.builder()
                .id(UUID.randomUUID())
                .name(realmName)
                .clinicSlug(clinicSlug)
                .issuer(keycloakResponse.get("issuer"))
                .adminUsername("admin")
                .adminTmpPassword("randomPassword123")
                .serviceClientId("mdental-frontend")
                .serviceClientSecret("clientSecret123")
                .build();

        when(realmRepository.save(any(Realm.class))).thenReturn(savedRealm);
        doNothing().when(outboxService).saveEvent(anyString(), any(UUID.class), anyString(), isNull(), any(Realm.class));

        // Act
        RealmResponse response = service.createRealm(realmName, clinicSlug);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIssuer()).isEqualTo("http://localhost:8080/realms/mdental-test");
        assertThat(response.getRealmName()).isEqualTo(realmName);
        assertThat(response.getKcRealmAdminUser()).isEqualTo("admin");
        assertThat(response.getTmpPassword()).isEqualTo("randomPassword123");

        ArgumentCaptor<Realm> realmCaptor = ArgumentCaptor.forClass(Realm.class);
        verify(realmRepository).save(realmCaptor.capture());

        Realm capturedRealm = realmCaptor.getValue();
        assertThat(capturedRealm.getName()).isEqualTo(realmName);
        assertThat(capturedRealm.getClinicSlug()).isEqualTo(clinicSlug);
        assertThat(capturedRealm.getAdminTmpPassword()).isEqualTo("randomPassword123");
        assertThat(capturedRealm.getServiceClientId()).isEqualTo("mdental-frontend");
        assertThat(capturedRealm.getServiceClientSecret()).isEqualTo("clientSecret123");

        verify(outboxService).saveEvent(eq("Realm"), any(UUID.class), eq("CREATED"), isNull(), any(Realm.class));
    }

    @Test
    void createRealm_ValidationFailure() {
        // Act & Assert
        assertThrows(ValidationException.class, () ->
                service.createRealm("invalid-realm", "test")
        );

        assertThrows(ValidationException.class, () ->
                service.createRealm("mdental-test", "INVALID")
        );

        verify(keycloakClient, never()).createRealm(anyString(), anyString());
        verify(realmRepository, never()).save(any(Realm.class));
    }

    @Test
    void regenerateAdminPassword_Success() {
        // Arrange
        String realmName = "mdental-test";
        String adminUsername = "admin";

        Realm existingRealm = Realm.builder()
                .id(UUID.randomUUID())
                .name(realmName)
                .clinicSlug("test")
                .issuer("http://localhost:8080/realms/mdental-test")
                .adminUsername("admin")
                .build();

        when(realmRepository.findByName(realmName)).thenReturn(Optional.of(existingRealm));

        Map<String, String> keycloakResponse = Map.of(
                "issuer", "http://localhost:8080/realms/mdental-test",
                "kcRealmAdminUser", adminUsername,
                "tmpPassword", "newPassword123"
        );

        when(keycloakClient.regenerateAdminPassword(realmName, adminUsername)).thenReturn(keycloakResponse);
        when(realmRepository.save(any(Realm.class))).thenReturn(existingRealm);

        // Act
        RealmResponse response = service.regenerateAdminPassword(realmName, adminUsername);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRealmName()).isEqualTo(realmName);
        assertThat(response.getKcRealmAdminUser()).isEqualTo(adminUsername);
        assertThat(response.getTmpPassword()).isEqualTo("newPassword123");

        ArgumentCaptor<Realm> realmCaptor = ArgumentCaptor.forClass(Realm.class);
        verify(realmRepository).save(realmCaptor.capture());

        Realm updatedRealm = realmCaptor.getValue();
        assertThat(updatedRealm.getAdminTmpPassword()).isEqualTo("newPassword123");

        verify(outboxService).saveEvent(eq("Realm"), any(UUID.class), eq("PASSWORD_ROTATED"), isNull(), any(Map.class));
    }

    @Test
    void regenerateAdminPassword_RealmNotFound() {
        // Arrange
        String realmName = "mdental-nonexistent";
        String adminUsername = "admin";

        when(realmRepository.findByName(realmName)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ValidationException.class, () ->
                service.regenerateAdminPassword(realmName, adminUsername)
        );

        verify(keycloakClient, never()).regenerateAdminPassword(anyString(), anyString());
        verify(realmRepository, never()).save(any(Realm.class));
    }
}