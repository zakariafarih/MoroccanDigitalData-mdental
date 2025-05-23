package org.mdental.security.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.mdental.security.jwt.JwtException;

/**
 * Exception for expired JWT tokens
 */
public class TokenExpiredException extends JwtException {
    public TokenExpiredException(String message) {
        super(message);
    }
}