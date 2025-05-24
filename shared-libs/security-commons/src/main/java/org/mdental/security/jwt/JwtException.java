package org.mdental.security.jwt;

import lombok.Getter;

/**
 * Base exception for JWT-related errors
 */
@Getter
public class JwtException extends RuntimeException {
    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}