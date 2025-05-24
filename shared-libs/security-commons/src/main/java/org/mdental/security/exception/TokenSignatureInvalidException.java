package org.mdental.security.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.mdental.security.jwt.JwtException;

/**
 * Exception for JWT tokens with invalid signatures
 */
public class TokenSignatureInvalidException extends JwtException {
    public TokenSignatureInvalidException(String message) {
        super(message);
    }
}