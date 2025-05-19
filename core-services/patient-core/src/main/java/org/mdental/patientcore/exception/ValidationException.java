package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class ValidationException extends BaseException {
    public ValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR);
    }
}
