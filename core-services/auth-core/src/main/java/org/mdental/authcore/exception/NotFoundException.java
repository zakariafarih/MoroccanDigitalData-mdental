package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND, cause);
    }
}