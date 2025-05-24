package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

/**
 * Exception thrown when a locked account is accessed.
 */
public class AccountLockedException extends BaseException {

    /**
     * Create a new account locked exception with a message.
     *
     * @param message the error message
     */
    public AccountLockedException(String message) {
        super(message, ErrorCode.USER_DISABLED);
    }

    /**
     * Create a new account locked exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public AccountLockedException(String message, Throwable cause) {
        super(message, ErrorCode.USER_DISABLED, cause);
    }
}