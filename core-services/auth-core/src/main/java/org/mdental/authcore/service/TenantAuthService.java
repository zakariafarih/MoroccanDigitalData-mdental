package org.mdental.authcore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.api.dto.UserInfoResponse;
import org.mdental.authcore.client.KeycloakClient;
import org.mdental.authcore.exception.NotFoundException;
import org.mdental.authcore.exception.ValidationException;
import org.mdental.authcore.model.entity.Outbox;
import org.mdental.authcore.model.entity.Realm;
import org.mdental.authcore.repository.OutboxRepository;
import org.mdental.authcore.repository.RealmRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAuthService {

    private final RealmRepository realmRepository;
    private final KeycloakClient keycloakClient;
    private final OutboxRepository outboxRepository;

    @Transactional
    public Map<String, Object> login(String tenantSlug, String username, String password) {
        log.debug("Authenticating user {} for tenant {}", username, tenantSlug);
        Realm realm = findRealmByTenantSlug(tenantSlug);
        return keycloakClient.directGrantLogin(realm.getName(), username, password);
    }

    @Transactional
    public Map<String, Object> refreshToken(String tenantSlug, String refreshToken) {
        log.debug("Refreshing token for tenant {}", tenantSlug);
        Realm realm = findRealmByTenantSlug(tenantSlug);
        return keycloakClient.refreshToken(realm.getName(), refreshToken);
    }

    @Transactional
    public void registerUser(String tenantSlug, String username, String email, String firstName, String lastName, String password, String role) {
        log.debug("Registering user {} for tenant {} with role {}", username, tenantSlug, role);

        // Validate username
        if (!username.matches("^[a-zA-Z0-9_-]{3,50}$")) {
            throw new ValidationException("Username must be 3-50 characters and contain only letters, numbers, hyphens, and underscores");
        }

        Realm realm = findRealmByTenantSlug(tenantSlug);

        // Default role to PATIENT if not specified
        String userRole = (role == null || role.trim().isEmpty()) ? "PATIENT" : role;

        // Create user in Keycloak
        keycloakClient.createUser(realm.getName(), username, email, firstName, lastName, password, userRole);

        // Publish event via outbox pattern
        Outbox outboxEntry = Outbox.builder()
                .aggregateType("User")
                .aggregateId(UUID.randomUUID()) // Using a placeholder UUID since we don't have the actual user ID
                .eventType("USER_REGISTERED")
                .payload(String.format(
                        "{\"username\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\",\"realmName\":\"%s\",\"role\":\"%s\"}",
                        username, email, firstName, lastName, realm.getName(), userRole))
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(outboxEntry);
    }

    @Transactional
    public void triggerPasswordReset(String tenantSlug, String email) {
        log.debug("Triggering password reset for email {} in tenant {}", email, tenantSlug);
        Realm realm = findRealmByTenantSlug(tenantSlug);
        keycloakClient.triggerResetEmail(realm.getName(), email);
    }

    public UserInfoResponse getCurrentUser(Jwt jwt) {
        String realm = jwt.getIssuer().getPath();           // "…/realms/<realm>"
        realm = realm.substring(realm.lastIndexOf('/') + 1);

        Set<String> roles = Collections.emptySet();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof java.util.Collection<?> raw) {
            roles = raw.stream().map(Object::toString).collect(Collectors.toSet());
        }

        return UserInfoResponse.builder()
                .sub(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .firstName(jwt.getClaimAsString("given_name"))
                .lastName(jwt.getClaimAsString("family_name"))
                .realm(realm)
                .roles(roles)
                .build();
    }

    private Realm findRealmByTenantSlug(String tenantSlug) {
        return realmRepository.findByClinicSlug(tenantSlug)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantSlug));
    }
}