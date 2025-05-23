package org.mdental.commons.exception;

import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.model.ApiError;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.error("Application exception: {}", ex.getMessage(), ex);

        ApiError error = ApiError.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(error);
        return new ResponseEntity<>(response, ex.getErrorCode().toHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation exception: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR)
                .message("Validation failed")
                .build();

        ex.getBindingResult().getAllErrors().forEach(err -> {
            String fieldName = err instanceof FieldError ? ((FieldError) err).getField() : err.getObjectName();
            String message = err.getDefaultMessage();
            error.addValidationError(fieldName, message);
        });

        ApiResponse<Void> response = ApiResponse.error(error);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
        log.error("Optimistic locking failure: {}", ex.getMessage(), ex);

        ApiError error = ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR)
                .message("Concurrent modification, retry")
                .build();

        ApiResponse<Void> response = ApiResponse.error(error);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ApiError error = ApiError.builder()
                .code(ErrorCode.GENERAL_ERROR)
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(error);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}