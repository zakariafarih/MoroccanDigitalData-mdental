package org.mdental.authcore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.authcore.api.dto.UserInfoResponse;
import org.mdental.authcore.client.KeycloakClient;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.model.entity.Outbox;
import org.mdental.authcore.model.entity.Realm;
import org.mdental.authcore.repository.OutboxRepository;
import org.mdental.authcore.repository.RealmRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAuthServiceTest {

    @Mock
    private RealmRepository realmRepository;

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private TenantAuthService tenantAuthService;

    private Realm testRealm;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        testRealm = Realm.builder()
                .id(UUID.randomUUID())
                .name("mdental-test")
                .clinicSlug("test")
                .issuer("http://localhost:9080/realms/mdental-test")
                .adminUsername("admin")
                .createdAt(Instant.now())
                .build();

        // Mock JWT with required claims for getCurrentUser method
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-123");
        claims.put("preferred_username", "testuser");
        claims.put("email", "test@example.com");
        claims.put("given_name", "Test");
        claims.put("family_name", "User");
        claims.put("realm_access", Map.of("roles", List.of("PATIENT", "USER")));

        mockJwt = Jwt.withTokenValue("token")
                .headers(h -> h.putAll(headers))
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .issuer("http://localhost:9080/realms/mdental-test")
                .build();
    }

    @Test
    void login_success() {
        // Arrange
        String tenantSlug = "test";
        String username = "user";
        String password = "password";
        Map<String, Object> expectedTokens = Map.of(
                "access_token", "eyJ...",
                "refresh_token", "eyJ...",
                "expires_in", 300
        );

        when(realmRepository.findByClinicSlug(tenantSlug)).thenReturn(Optional.of(testRealm));
        when(keycloakClient.directGrantLogin(testRealm.getName(), username, password)).thenReturn(expectedTokens);

        // Act
        Map<String, Object> result = tenantAuthService.login(tenantSlug, username, password);

        // Assert
        assertThat(result).isEqualTo(expectedTokens);
        verify(realmRepository).findByClinicSlug(tenantSlug);
        verify(keycloakClient).directGrantLogin(testRealm.getName(), username, password);
    }

    @Test
    void login_tenantNotFound() {
        // Arrange
        String tenantSlug = "nonexistent";
        when(realmRepository.findByClinicSlug(tenantSlug)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                tenantAuthService.login(tenantSlug, "user", "password")
        );
        verify(realmRepository).findByClinicSlug(tenantSlug);
        verifyNoInteractions(keycloakClient);
    }

    @Test
    void refreshToken_success() {
        // Arrange
        String tenantSlug = "test";
        String refreshToken = "eyJ...";
        Map<String, Object> expectedTokens = Map.of(
                "access_token", "eyJ...",
                "refresh_token", "eyJ...",
                "expires_in", 300
        );

        when(realmRepository.findByClinicSlug(tenantSlug)).thenReturn(Optional.of(testRealm));
        when(keycloakClient.refreshToken(testRealm.getName(), refreshToken)).thenReturn(expectedTokens);

        // Act
        Map<String, Object> result = tenantAuthService.refreshToken(tenantSlug, refreshToken);

        // Assert
        assertThat(result).isEqualTo(expectedTokens);
        verify(realmRepository).findByClinicSlug(tenantSlug);
        verify(keycloakClient).refreshToken(testRealm.getName(), refreshToken);
    }

    @Test
    void registerUser_success() {
        // Arrange
        String tenantSlug = "test";
        String username = "newuser";
        String email = "new@example.com";
        String firstName = "New";
        String lastName = "User";
        String password = "password123";

        when(realmRepository.findByClinicSlug(tenantSlug)).thenReturn(Optional.of(testRealm));
        doNothing().when(keycloakClient).createUser(
                testRealm.getName(), username, email, firstName, lastName, password);

        // Act
        tenantAuthService.registerUser(tenantSlug, username, email, firstName, lastName, password);

        // Assert
        verify(realmRepository).findByClinicSlug(tenantSlug);
        verify(keycloakClient).createUser(testRealm.getName(), username, email, firstName, lastName, password);

        // Verify outbox entry was created
        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        Outbox savedOutbox = outboxCaptor.getValue();
        assertThat(savedOutbox.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(savedOutbox.getAggregateType()).isEqualTo("User");
        assertThat(savedOutbox.getPayload()).contains(username, email, firstName, lastName, testRealm.getName());
    }

    @Test
    void triggerPasswordReset_success() {
        // Arrange
        String tenantSlug = "test";
        String email = "test@example.com";

        when(realmRepository.findByClinicSlug(tenantSlug)).thenReturn(Optional.of(testRealm));
        doNothing().when(keycloakClient).triggerResetEmail(testRealm.getName(), email);

        // Act
        tenantAuthService.triggerPasswordReset(tenantSlug, email);

        // Assert
        verify(realmRepository).findByClinicSlug(tenantSlug);
        verify(keycloakClient).triggerResetEmail(testRealm.getName(), email);
    }

    @Test
    void getCurrentUser_success() {
        // Act
        UserInfoResponse response = tenantAuthService.getCurrentUser(mockJwt);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSub()).isEqualTo("user-123");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
        assertThat(response.getRealm()).isEqualTo("mdental-test");
        assertThat(response.getRoles()).containsExactlyInAnyOrder("PATIENT", "USER");
    }
}