package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when a duplicate resource is detected.
 */
public class DuplicateResourceException extends BaseException {

    /**
     * Create a new duplicate resource exception with a message.
     *
     * @param message the error message
     */
    public DuplicateResourceException(String message) {
        super(message, ErrorCode.DUPLICATE_RESOURCE);
    }

    /**
     * Create a new duplicate resource exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, ErrorCode.DUPLICATE_RESOURCE, cause);
    }
}