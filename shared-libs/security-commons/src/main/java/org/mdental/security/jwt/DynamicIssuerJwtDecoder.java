package org.mdental.security.jwt;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicIssuerJwtDecoder implements JwtDecoder {

    private final List<String> allowedIssuerPatterns;
    private final Map<String, JwtDecoder> cache = new ConcurrentHashMap<>();

    public DynamicIssuerJwtDecoder(List<String> allowedIssuerPatterns) {
        this.allowedIssuerPatterns = allowedIssuerPatterns;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        String issuer = extractIssuer(token);
        JwtDecoder decoder = cache.computeIfAbsent(issuer, this::buildDecoder);
        try {
            Jwt jwt = decoder.decode(token);
            // wicket: in your test the mock will return null, so explicitly treat that as an error
            if (jwt == null) {
                throw new JwtException("Decoded JWT is null");
            }
            return jwt;
        } catch (JwtException e) {
            // re-throw so the test’s catch sees it
            throw e;
        } catch (Exception e) {
            // wrap any other exception
            throw new JwtException("Failed to decode JWT", e);
        }
    }

    /* ---------- helpers ---------- */

    protected JwtDecoder buildDecoder(String issuer) {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> realmValidator = new KeycloakRealmJwtIssuerValidator(allowedIssuerPatterns);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, realmValidator));
        return decoder;
    }

    public String extractIssuerSafely(String token) {
        try {
            return extractIssuer(token);
        } catch (JwtException e) {
            throw e;
        }
    }

    private String extractIssuer(String token) throws JwtException {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            return jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            throw new JwtException("Invalid JWT token structure", e);
        }
    }
}