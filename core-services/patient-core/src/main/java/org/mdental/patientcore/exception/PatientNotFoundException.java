package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class PatientNotFoundException extends BaseException {
    public PatientNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}