package org.mdental.patientcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class PortalAccountAlreadyExistsException extends BaseException {
    public PortalAccountAlreadyExistsException(String message) {
        super(message, ErrorCode.DUPLICATE_RESOURCE);
    }
}
