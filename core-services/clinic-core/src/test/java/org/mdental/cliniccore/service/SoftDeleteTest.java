package org.mdental.cliniccore.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdental.cliniccore.config.TestConfig;
import org.mdental.cliniccore.model.dto.CreateClinicRequest;
import org.mdental.cliniccore.model.entity.Clinic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class SoftDeleteTest {

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private EntityManager entityManager;

    private SecurityContext originalContext;

    @BeforeEach
    void setupSecurityContext() {
        // Save original context
        originalContext = SecurityContextHolder.getContext();

        // Create new context with SUPER_ADMIN role
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/test")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                "system-admin"
        );
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void restoreSecurityContext() {
        SecurityContextHolder.setContext(originalContext);
    }

    @Test
    @Transactional
    public void testSoftDelete() {
        // Test remains unchanged
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        CreateClinicRequest request = CreateClinicRequest.builder()
                .name("Test Soft Delete Clinic " + uniqueId)
                .slug("test-soft-delete-" + uniqueId)
                .realm("test-soft-delete-" + uniqueId)
                .build();

        Clinic clinic = clinicService.createClinic(request);
        UUID clinicId = clinic.getId();

        entityManager.flush();

        Clinic found = clinicService.getClinicById(clinicId);
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(clinicId);

        clinicService.deleteClinic(clinicId);

        entityManager.flush();
        entityManager.clear();

        assertThat(clinicService.findClinicByRealm(request.getRealm())).isEmpty();

        Clinic deletedClinic = entityManager
                .createQuery("SELECT c FROM Clinic c WHERE c.id = :id", Clinic.class)
                .setParameter("id", clinicId)
                .getSingleResult();

        assertThat(deletedClinic).isNotNull();
        assertThat(deletedClinic.getDeletedAt()).isNotNull();
        assertThat(deletedClinic.getDeletedBy()).isNotNull();
    }
}