package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class EmergencyContactNotFoundException extends BaseException {
    public EmergencyContactNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}