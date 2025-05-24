package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when a resource is not found.
 */
public class NotFoundException extends BaseException {

    /**
     * Create a new not found exception with a message.
     *
     * @param message the error message
     */
    public NotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * Create a new not found exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND, cause);
    }
}
