package org.mdental.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.regex.Pattern;

public class KeycloakRealmJwtIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_REALM =
            new OAuth2Error("invalid_issuer", "Token issuer not allowed", null);

    private final List<Pattern> compiledPatterns;

    public KeycloakRealmJwtIssuerValidator(List<String> allowedPatterns) {
        this.compiledPatterns = allowedPatterns.stream()
                .map(this::compilePattern)
                .toList();
    }

    private Pattern compilePattern(String pattern) {
        // Convert pattern with asterisk suffix to regex pattern
        if (pattern.endsWith("*")) {
            String prefix = Pattern.quote(pattern.substring(0, pattern.length() - 1));
            return Pattern.compile("^" + prefix + ".*$");
        } else {
            // Exact match for patterns without wildcard
            return Pattern.compile("^" + Pattern.quote(pattern) + "$");
        }
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String iss = jwt.getIssuer().toString();
        boolean ok = compiledPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(iss).matches());

        return ok ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_REALM);
    }
}