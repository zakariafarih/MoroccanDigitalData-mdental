package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class IdentifierNotFoundException extends BaseException {
    public IdentifierNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}