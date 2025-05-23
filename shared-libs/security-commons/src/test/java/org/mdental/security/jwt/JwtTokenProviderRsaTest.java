package org.mdental.security.jwt;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.autoconfig.JwtProps;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**

 Tests for JwtTokenProvider with RSA (asymmetric) key pair
 */
class JwtTokenProviderRsaTest {

    private JwtTokenProvider tokenProvider;
    private JwtProps jwtProps;
    private UUID userId;
    private UUID tenantId;
    private Set<Role> roles;
    private KeyPair keyPair;
    private Clock fixedClock;

    @BeforeEach
    void setup() {
        jwtProps = new JwtProps();
        jwtProps.setIssuer("mdental.test");
        jwtProps.setAccessTtl(3600);
        jwtProps.setRefreshTtl(86400);
        // Generate an RSA key pair
        keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

        // Encode the keys
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        jwtProps.setPublicKey(publicKeyBase64);
        jwtProps.setPrivateKey(privateKeyBase64);

        // Create a fixed clock for predictable timestamps
        Instant now = Instant.parse("2023-01-01T12:00:00Z");
        fixedClock = Clock.fixed(now, ZoneOffset.UTC);

        tokenProvider = new JwtTokenProvider(jwtProps, fixedClock);

        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        roles = Set.of(Role.DOCTOR, Role.CLINIC_ADMIN);
    }

    @Test
    void createAndValidateToken_withRsaKeys_shouldSucceed() {
// Arrange
        String username = "doctor.smith";
        String email = "doctor.smith@mdental.org";
        // Act - Create token
        String token = tokenProvider.createToken(
                userId, username, email, tenantId, roles);

        // Assert - Valid token
        assertTrue(tokenProvider.validateToken(token));

        // Act - Parse token
        AuthPrincipal principal = tokenProvider.parseToken(token);

        // Assert - Correct information
        assertEquals(userId, principal.id());
        assertEquals(tenantId, principal.tenantId());
        assertEquals(username, principal.username());
        assertEquals(email, principal.email());
        assertEquals(roles, principal.roles());
    }

    @Test
    void createToken_withRsaSigning_shouldUseRs256Algorithm() {
// Arrange
        String username = "doctor.smith";
        String email = "doctor.smith@mdental.org";
        // Act
        String token = tokenProvider.createToken(
                userId, username, email, tenantId, roles);

        // Assert - Check token header
        String[] parts = token.split("\\.");
        String header = new String(Base64.getUrlDecoder().decode(parts[0]));

        assertTrue(header.contains("RS256"), "Token should use RS256 algorithm");
    }

    @Test
    void refreshToken_withRsaKeys_shouldCreateNewValidToken() {
// Arrange
        String refreshToken = tokenProvider.createRefreshToken(userId, tenantId);
        // Act
        String newToken = tokenProvider.refreshToken(refreshToken);

        // Assert
        assertNotNull(newToken);
        assertTrue(tokenProvider.validateToken(newToken));

        AuthPrincipal principal = tokenProvider.parseToken(newToken);
        assertEquals(userId, principal.id());
        assertEquals(tenantId, principal.tenantId());
    }

    @Test
    void isRsa_shouldReturnTrue_whenBothKeysAreConfigured() {
// Assert
        assertTrue(jwtProps.isRsa(), "isRsa should return true when both keys are set");
    }
}