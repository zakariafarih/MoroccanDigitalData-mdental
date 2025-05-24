package org.mdental.security.jwt;

/**

 Canonical claim names used across MDental services.
 */
public enum JwtClaim {
    SUBJECT("sub"),
    USERNAME("preferred_username"),
    EMAIL("email"),
    TENANT_ID("tenant_id"),
    ROLES("roles"),
    TOKEN_TYPE("token_type");

    private final String value;

    JwtClaim(String v) {
        this.value = v;
    }

    @Override
    public String toString() {
        return value;
    }
}