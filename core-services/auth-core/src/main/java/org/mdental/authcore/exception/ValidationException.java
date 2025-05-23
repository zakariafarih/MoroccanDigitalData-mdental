package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends BaseException {

    /**
     * Create a new validation exception with a message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR);
    }

    /**
     * Create a new validation exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, ErrorCode.VALIDATION_ERROR, cause);
    }
}