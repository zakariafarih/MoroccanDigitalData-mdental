package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when a token is invalid.
 */
public class InvalidTokenException extends BaseException {

    /**
     * Create a new invalid token exception with a message.
     *
     * @param message the error message
     */
    public InvalidTokenException(String message) {
        super(message, ErrorCode.INVALID_TOKEN);
    }

    /**
     * Create a new invalid token exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(message, ErrorCode.INVALID_TOKEN, cause);
    }
}