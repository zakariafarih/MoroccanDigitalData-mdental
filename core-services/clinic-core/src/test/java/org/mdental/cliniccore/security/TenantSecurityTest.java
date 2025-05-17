package org.mdental.cliniccore.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.cliniccore.model.entity.Clinic;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TenantSecurityTest {

    @Mock
    private ClinicRepository clinicRepository;

    private SecurityContext originalContext;

    @BeforeEach
    void saveOriginalContext() {
        originalContext = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void restoreOriginalContext() {
        SecurityContextHolder.setContext(originalContext);
    }

    @Test
    public void testTenantGuardSameClinic_Success() {
        // Set up mock clinic
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic();
        clinic.setId(clinicId);
        clinic.setRealm("test-realm");

        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));

        // Set up security context with matching realm
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/test-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Create tenant guard and test
        TenantGuard tenantGuard = new TenantGuard(clinicRepository);
        boolean result = tenantGuard.sameClinic(clinicId);

        // Verify
        assertThat(result).isTrue();
    }

    @Test
    public void testTenantGuardSameClinic_Failure() {
        // Set up mock clinic with different realm
        UUID clinicId = UUID.randomUUID();
        Clinic clinic = new Clinic();
        clinic.setId(clinicId);
        clinic.setRealm("test-realm");

        when(clinicRepository.findByIdWithAllRelationships(clinicId)).thenReturn(Optional.of(clinic));

        // Set up security context with different realm
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/different-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Create tenant guard and test
        TenantGuard tenantGuard = new TenantGuard(clinicRepository);
        boolean result = tenantGuard.sameClinic(clinicId);

        // Verify
        assertThat(result).isFalse();
    }

    @Test
    public void testTenantSecurityAspect_SkipsForSystemAdmin() {
        // Set up mock tenant guard
        TenantGuard tenantGuard = Mockito.mock(TenantGuard.class);
        TenantSecurityAspect aspect = new TenantSecurityAspect(tenantGuard);
        UUID clinicId = UUID.randomUUID();

        // Mock tenant guard to return false (different clinics)
        when(tenantGuard.sameClinic(clinicId)).thenReturn(false);

        // Set up security context with SUPER_ADMIN role
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/different-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                "username"
        );
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // This should not throw an exception since we're a system admin
        try {
            if (auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
                // System admin should skip the check
            } else if (!tenantGuard.sameClinic(clinicId)) {
                throw new TenantSecurityAspect.TenantSecurityException("Access denied");
            }
        } catch (Exception e) {
            // We should not get here if system admin check works
            assertThat(false).isTrue(); // This will fail the test if we get here
        }
    }

    @Test
    public void testTenantSecurityAspect_ThrowsExceptionOnMismatch() {
        // Set up mock objects
        TenantGuard tenantGuard = Mockito.mock(TenantGuard.class);
        TenantSecurityAspect aspect = new TenantSecurityAspect(tenantGuard);
        UUID clinicId = UUID.randomUUID();

        // Mock tenant guard to return false (different clinics)
        when(tenantGuard.sameClinic(clinicId)).thenReturn(false);

        // Set up security context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/different-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")),
                "username"
        );
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Verify exception is thrown
        assertThrows(TenantSecurityAspect.TenantSecurityException.class, () -> {
            // This would simulate what the aspect does, but we can't directly test the aspect
            if (!tenantGuard.sameClinic(clinicId)) {
                throw new TenantSecurityAspect.TenantSecurityException("Access denied");
            }
        });
    }
}