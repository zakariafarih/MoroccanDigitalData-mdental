package org.mdental.commons.model;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // General errors (1000-1999)
    GENERAL_ERROR(1000, HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(1001, HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(1003, HttpStatus.CONFLICT),
    UNAUTHORIZED(1004, HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1005, HttpStatus.FORBIDDEN),

    // Auth service errors (2000-2999)
    INVALID_CREDENTIALS(2000, HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(2001, HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(2002, HttpStatus.UNAUTHORIZED),
    USER_DISABLED(2003, HttpStatus.FORBIDDEN),

    // Clinic service errors (3000-3999)
    APPOINTMENT_CONFLICT(3000, HttpStatus.CONFLICT),
    PATIENT_NOT_FOUND(3001, HttpStatus.NOT_FOUND),
    DOCTOR_NOT_AVAILABLE(3002, HttpStatus.BAD_REQUEST),
    CLINIC_CLOSED(3003, HttpStatus.SERVICE_UNAVAILABLE);

    @Getter
    private final int value;
    private final HttpStatus httpStatus;

    ErrorCode(int value, HttpStatus httpStatus) {
        this.value = value;
        this.httpStatus = httpStatus;
    }

    public HttpStatus toHttpStatus() {
        return httpStatus;
    }
}