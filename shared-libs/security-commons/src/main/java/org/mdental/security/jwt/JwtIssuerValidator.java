package org.mdental.security.jwt;

/**
 * Contract for validating the <i>iss</i> (issuer) claim of a JWT.
 * Kept in security-commons so that any service can plug in its own.
 */
@FunctionalInterface
public interface JwtIssuerValidator {
    /**
     * @param issuer the <code>iss</code> claim value
     * @return {@code true} if we trust this issuer
     */
    boolean validate(String issuer);
}
