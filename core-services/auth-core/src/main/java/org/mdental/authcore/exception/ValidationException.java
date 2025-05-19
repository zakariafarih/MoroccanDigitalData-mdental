package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, ErrorCode.VALIDATION_ERROR, cause);
    }
}