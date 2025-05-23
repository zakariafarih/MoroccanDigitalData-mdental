package org.mdental.authcore.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.security.jwt.JwtClaim;
import org.mdental.security.jwt.JwtTokenProvider;

/**
 * JWT token provider that uses RSA keys for signing.
 */
@Slf4j
public class RsaJwtTokenProvider extends JwtTokenProvider {
    private final String issuer;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String keyId;

    /**
     * Constructor.
     *
     * @param issuer the issuer
     * @param privateKey the private key
     * @param publicKey the public key
     * @param accessTtlSeconds the access token TTL in seconds
     * @param refreshTtlSeconds the refresh token TTL in seconds
     * @param keyId the key ID
     */
    public RsaJwtTokenProvider(
            String issuer,
            RSAPrivateKey privateKey,
            RSAPublicKey publicKey,
            long accessTtlSeconds,
            long refreshTtlSeconds,
            String keyId) {
        super();
        this.issuer = issuer;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.keyId = keyId;
    }

    /**
     * Create a JWT for the given user.
     *
     * @param id The user's unique ID
     * @param username The user's username
     * @param email The user's email
     * @param tenantId The tenant (clinic) ID
     * @param roles Set of user roles
     * @return The signed JWT token
     */
    @Override
    public String createToken(UUID id, String username, String email, UUID tenantId, Set<Role> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTtlSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(), id.toString());
        claims.put(JwtClaim.USERNAME.toString(), username);
        claims.put(JwtClaim.EMAIL.toString(), email);
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.ROLES.toString(), roles.stream()
                .map(Role::name)
                .collect(Collectors.toList()));

        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", keyId);

        return Jwts.builder()
                .setHeader(headers)
                .setClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Create a refresh token.
     *
     * @param id The user's unique ID
     * @param tenantId The tenant (clinic) ID
     * @return The refresh token
     */
    @Override
    public String createRefreshToken(UUID id, UUID tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTtlSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(), id.toString());
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.TOKEN_TYPE.toString(), "refresh");

        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", keyId);

        return Jwts.builder()
                .setHeader(headers)
                .setClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Parse the token and extract user details.
     *
     * @param token The JWT token to parse
     * @return User details
     */
    @Override
    public AuthPrincipal parseToken(String token) {
        // Implement token parsing logic
        // This would extract claims from the JWT and construct an AuthPrincipal
        return null;
    }

    /**
     * Validate the token's structure and signature.
     *
     * @param token The token to validate
     * @return true if valid
     */
    @Override
    public boolean validateToken(String token) {
        // Implement token validation logic
        // This would verify the token signature and expiration
        return true;
    }
}