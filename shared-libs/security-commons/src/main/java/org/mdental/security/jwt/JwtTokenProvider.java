package org.mdental.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.model.AuthPrincipal;
import org.mdental.commons.model.Role;
import org.mdental.commons.security.RoleUtils;
import org.mdental.security.autoconfig.JwtProps;
import org.mdental.security.exception.TokenExpiredException;
import org.mdental.security.exception.TokenInvalidException;
import org.mdental.security.exception.TokenSignatureInvalidException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**

 Provider for JWT token operations including signing, verification, parsing, and refresh.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {
    private static final String AUTH_PREFIX = "Bearer ";

    @NonNull
    private final JwtProps jwtProps;

    @NonNull
    private final Clock clock;

    /**

     Create a JWT for the given user

     @param id The user's unique ID

     @param username The user's username

     @param email The user's email

     @param tenantId The tenant (clinic) ID

     @param roles Set of user roles

     @return The signed JWT token
     */
    @Schema(description = "Generate a signed JWT token")
    public String createToken(UUID id, String username, String email, UUID tenantId, Set<Role> roles) {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(roles, "Roles cannot be null");

        Instant now = clock.instant();
        Instant expiryDate = now.plus(jwtProps.getAccessTtl(), ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(), id.toString());
        claims.put(JwtClaim.USERNAME.toString(), username);
        claims.put(JwtClaim.EMAIL.toString(), email);
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.ROLES.toString(), roles.stream()
                .map(Role::name)
                .collect(Collectors.toList()));

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .setIssuer(jwtProps.getIssuer());

        if (jwtProps.isRsa()) {
// RSA signing
            builder.signWith(getPrivateKey(), SignatureAlgorithm.RS256);
        } else {
// HMAC signing
            builder.signWith(getSecretKey(), SignatureAlgorithm.HS256);
        }

        return builder.compact();
    }

    /**

     Create a JWT specifically for refresh purposes

     @param id The user's unique ID

     @param tenantId The tenant (clinic) ID

     @return A refresh token
     */
    public String createRefreshToken(UUID id, UUID tenantId) {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null");

        Instant now = clock.instant();
        Instant expiryDate = now.plus(jwtProps.getRefreshTtl(), ChronoUnit.SECONDS);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaim.SUBJECT.toString(), id.toString());
        claims.put(JwtClaim.TENANT_ID.toString(), tenantId.toString());
        claims.put(JwtClaim.TOKEN_TYPE.toString(), "refresh");

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .setIssuer(jwtProps.getIssuer());

        if (jwtProps.isRsa()) {
            builder.signWith(getPrivateKey(), SignatureAlgorithm.RS256);
        } else {
            builder.signWith(getSecretKey(), SignatureAlgorithm.HS256);
        }

        return builder.compact();
    }

    /**

     Parse the provided token and extract user details into AuthPrincipal

     @param token The JWT token to parse

     @return User details

     @throws JwtException if the token is invalid
     */
    public AuthPrincipal parseToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null");

        Claims claims = extractClaims(token);

        UUID id = UUID.fromString(claims.getSubject());
        String username = (String) claims.get(JwtClaim.USERNAME.toString());
        String email = (String) claims.get(JwtClaim.EMAIL.toString());
        UUID tenantId = UUID.fromString((String) claims.get(JwtClaim.TENANT_ID.toString()));

        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) claims.get(JwtClaim.ROLES.toString());
        Set<Role> roles = Collections.emptySet();

        if (roleNames != null && !roleNames.isEmpty()) {
            roles = roleNames.stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
        }

        return new AuthPrincipal(id, tenantId, username, email, roles);
    }

    /**

     Validate the token's structure and signature

     @param token The token to validate

     @return true if valid

     @throws JwtException if the token is invalid
     */
    public boolean validateToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null");

        try {
            if (jwtProps.isRsa()) {
                Jwts.parserBuilder()
                        .setSigningKey(getPublicKey())
                        .build()
                        .parseClaimsJws(token);
            } else {
                Jwts.parserBuilder()
                        .setSigningKey(getSecretKey())
                        .build()
                        .parseClaimsJws(token);
            }
            return true;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("JWT token has expired");
        } catch (SignatureException e) {
            throw new TokenSignatureInvalidException("Invalid JWT signature");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new TokenInvalidException("Invalid JWT token");
        }
    }

    /**

     Refresh an existing token

     @param refreshToken The refresh token

     @return A new access token

     @throws JwtException if the refresh token is invalid
     */
    public String refreshToken(String refreshToken) {
        Objects.requireNonNull(refreshToken, "Refresh token cannot be null");

        Claims claims = extractClaims(refreshToken);

// Ensure it's a refresh token
        String tokenType = (String) claims.get(JwtClaim.TOKEN_TYPE.toString());
        if (!"refresh".equals(tokenType)) {
            throw new TokenInvalidException("Not a refresh token");
        }

        UUID id = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString((String) claims.get(JwtClaim.TENANT_ID.toString()));

// Typically, you would revalidate the user from a database here
// This implementation assumes the refresh token itself contains enough info

        return createToken(
                id,
                "refreshed_user", // You'd typically load these from a repository
                "refreshed@example.com",
                tenantId,
                Collections.emptySet()
        );
    }

    /**

     Extract the token from the Authorization header
     @param authHeader The Authorization header value
     @return The token, or empty Optional if not present
     */
    public Optional<String> extractTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(AUTH_PREFIX)) {
            return Optional.of(authHeader.substring(AUTH_PREFIX.length()));
        }
        return Optional.empty();
    }
    /**

     Extract user authorities from the token

     @param token The JWT token

     @return List of GrantedAuthority objects
     */
    public Collection<GrantedAuthority> getAuthorities(String token) {
        Objects.requireNonNull(token, "Token cannot be null");

        Claims claims = extractClaims(token);

        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) claims.get(JwtClaim.ROLES.toString());
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Role> roles = roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        return RoleUtils.toAuthorities(roles);
    }

// Private helper methods

    private Claims extractClaims(String token) {
        try {
            if (jwtProps.isRsa()) {
                return Jwts.parserBuilder()
                        .setSigningKey(getPublicKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } else {
                return Jwts.parserBuilder()
                        .setSigningKey(getSecretKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            }
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("JWT token has expired");
        } catch (SignatureException e) {
            throw new TokenSignatureInvalidException("Invalid JWT signature");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new TokenInvalidException("Invalid JWT token: " + e.getMessage());
        }
    }

    private SecretKey getSecretKey() {
        if (jwtProps.getSecret() == null || jwtProps.getSecret().isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(jwtProps.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private PublicKey getPublicKey() {
        try {
            if (jwtProps.getPublicKey() == null || jwtProps.getPublicKey().isBlank()) {
                throw new IllegalStateException("JWT public key is not configured");
            }
            byte[] keyBytes = Base64.getDecoder().decode(jwtProps.getPublicKey());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key", e);
        }
    }

    private PrivateKey getPrivateKey() {
        try {
            if (jwtProps.getPrivateKey() == null || jwtProps.getPrivateKey().isBlank()) {
                throw new IllegalStateException("JWT private key is not configured");
            }
            byte[] keyBytes = Base64.getDecoder().decode(jwtProps.getPrivateKey());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }
}