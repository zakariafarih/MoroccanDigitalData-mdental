package org.mdental.cliniccore.security;

import lombok.RequiredArgsConstructor;
import org.mdental.cliniccore.repository.ClinicRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantGuard {

    private final ClinicRepository clinicRepository;

    /**
     * Checks if the current user belongs to the clinic specified by the ID
     */
    public boolean sameClinic(UUID clinicId) {
        String currentRealm = getCurrentRealm();
        if (currentRealm == null) {
            return false;
        }

        return clinicRepository.findByIdWithAllRelationships(clinicId)
                .map(clinic -> clinic.getRealm().equals(currentRealm))
                .orElse(false);
    }

    /**
     * Gets the current realm from JWT issuer
     */
    public String getCurrentRealm() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
            String issuer = jwt.getIssuer().toString();

            // Extract realm from issuer URL pattern: https://auth.mdental.org/realms/{realm}
            String[] parts = issuer.split("/realms/");
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }
}