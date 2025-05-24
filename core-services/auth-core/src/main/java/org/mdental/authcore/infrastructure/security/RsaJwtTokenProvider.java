package org.mdental.authcore.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
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
import org.mdental.security.autoconfig.JwtProps;
import org.mdental.security.jwt.JwtClaim;
import org.mdental.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final JWKSet jwkSet;

    public RsaJwtTokenProvider(
            JwtProps jwtProps,
            Clock clock,
            String issuer,
            RSAPrivateKey privateKey,
            RSAPublicKey publicKey,
            long accessTtlSeconds,
            long refreshTtlSeconds,
            String keyId,
            @Autowired(required = false) JWKSet jwkSet) {
        super(jwtProps, clock);
        this.issuer = issuer;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.keyId = keyId;
        this.jwkSet = jwkSet;
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
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(accessTtlSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(),   id.toString());
        claims.put(JwtClaim.USERNAME.toString(),  username);
        claims.put(JwtClaim.EMAIL.toString(),     email);
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.ROLES.toString(),
                roles.stream().map(Role::name).collect(Collectors.toList()));

        Map<String, Object> headers = Map.of("kid", keyId);

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
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(refreshTtlSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(),   id.toString());
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.TOKEN_TYPE.toString(), "refresh");

        Map<String, Object> headers = Map.of("kid", keyId);

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
        // Delegate to the parent, which will verify signature with the publicKey
        return super.parseToken(token);
    }

    /**
     * Validate the token's structure and signature.
     *
     * @param token The token to validate
     * @return true if valid
     */
    @Override
    public boolean validateToken(String token) {
        try {
            // Extract kid from token header
            JwsHeader<?> header = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getHeader();

            String tokenKid = header.getKeyId();

            // If jwkSet is available and kid doesn't match our current key,
            // try to get the key from the JWKSet
            if (jwkSet != null && tokenKid != null && !keyId.equals(tokenKid)) {
                RSAKey key = (RSAKey) jwkSet.getKeyByKeyId(tokenKid);
                if (key != null) {
                    RSAPublicKey alternatePublicKey = key.toRSAPublicKey();
                    Jwts.parserBuilder()
                            .setSigningKey(alternatePublicKey)
                            .build()
                            .parseClaimsJws(token);
                    return true;
                }
            }

            // Fall back to default validation with current key
            return super.validateToken(token);
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}