package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends BaseException {

    /**
     * Create a new authentication exception with a message.
     *
     * @param message the error message
     */
    public AuthenticationException(String message) {
        super(message, ErrorCode.INVALID_CREDENTIALS);
    }

    /**
     * Create a new authentication exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_CREDENTIALS, cause);
    }
}