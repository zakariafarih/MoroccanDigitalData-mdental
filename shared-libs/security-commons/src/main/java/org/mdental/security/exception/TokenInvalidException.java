package org.mdental.security.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.mdental.security.jwt.JwtException;

/**
 * Exception for invalid JWT tokens
 */
public class TokenInvalidException extends JwtException {
    public TokenInvalidException(String message) {
        super(message);
    }
}