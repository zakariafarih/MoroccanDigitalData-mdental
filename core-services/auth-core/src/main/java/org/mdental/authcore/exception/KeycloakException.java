package org.mdental.authcore.exception;

import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;

public class KeycloakException extends BaseException {

    public KeycloakException(String message) {
        super(message, ErrorCode.GENERAL_ERROR);
    }

    public KeycloakException(String message, Throwable cause) {
        super(message, ErrorCode.GENERAL_ERROR, cause);
    }
}