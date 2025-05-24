package org.mdental.cliniccore.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.mdental.commons.exception.BaseException;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class FallbackController {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("Validation error: {}", e.getMessage(), e);
        return ApiResponse.error(ErrorCode.VALIDATION_ERROR, e.getMessage());
    }

    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ApiResponse<Void> handleBaseException(BaseException e, HttpServletRequest request) {
        log.error("API error: {} - {}", e.getErrorCode(), e.getMessage(), e);
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ApiResponse<Void> handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ApiResponse.error(ErrorCode.GENERAL_ERROR, "An unexpected error occurred");
    }
}