package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class PortalAccountNotFoundException extends BaseException {
    public PortalAccountNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}