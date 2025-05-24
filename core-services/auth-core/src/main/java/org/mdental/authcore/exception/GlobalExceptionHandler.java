package org.mdental.authcore.exception;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ApiError;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountLockedException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MeterRegistry meterRegistry;

    /**
     * Handle application-specific exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        if (ex instanceof ValidationException || ex instanceof NotFoundException) {
            log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            log.error("Application exception on [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        }

        // Track error metrics
        meterRegistry.counter("auth.errors", "type", ex.getClass().getSimpleName()).increment();

        HttpStatus httpStatus = ex.getErrorCode().toHttpStatus();

        return ResponseEntity
                .status(httpStatus)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Handle validation exceptions.
     *
     * @param ex the validation exception
     * @param request the HTTP request
     * @return the error response with validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error on [{} {}]", request.getMethod(), request.getRequestURI());

        // Extract field errors into a map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        // Create API error with validation details
        ApiError apiError = ApiError.builder()
                .code(ErrorCode.VALIDATION_ERROR)
                .message("Validation failed")
                .build();

        errors.forEach(apiError::addValidationError);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(apiError));
    }

    /**
     * Handle authentication exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication exception on [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        meterRegistry.counter("auth.errors", "type", "authentication").increment();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS, ex.getMessage()));
    }

    /**
     * Handle account locked exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(
            AccountLockedException ex, HttpServletRequest request) {
        log.warn("Account locked exception on [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());

        meterRegistry.counter("auth.errors", "type", "account_locked").increment();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.USER_DISABLED, ex.getMessage()));
    }

    /**
     * Handle bad credentials exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials exception on [{} {}]", request.getMethod(), request.getRequestURI());

        meterRegistry.counter("auth.errors", "type", "bad_credentials").increment();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password"));
    }

    /**
     * Handle all uncaught exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{} {}]", request.getMethod(), request.getRequestURI(), ex);

        meterRegistry.counter("auth.errors", "type", "unhandled").increment();

        // For security, don't expose implementation details in production
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.GENERAL_ERROR, "An unexpected error occurred"));
    }

    /**
     * Handle access denied exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(AccessDeniedException.class)
   public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
           AccessDeniedException ex, HttpServletRequest request) {
               log.warn("Access denied exception on [{} {}]: {}",
                       request.getMethod(), request.getRequestURI(), ex.getMessage());
               meterRegistry.counter("auth.errors", "type", "access_denied").increment();
               return ResponseEntity
                       .status(HttpStatus.FORBIDDEN)
                       .body(ApiResponse.error(ErrorCode.ACCESS_DENIED, "Access denied"));
    }

    /**
     * Handle invalid token exceptions.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(InvalidTokenException.class)
   public ResponseEntity<ApiResponse<Void>> handleInvalidToken(
           InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Invalid token exception on [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        meterRegistry.counter("auth.errors", "type", "invalid_token").increment();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.INVALID_TOKEN, ex.getMessage()));
    }

    /**
     * Handle data integrity violations.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return the error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
               log.warn("Data integrity violation on [{} {}]: {}",
                       request.getMethod(), request.getRequestURI(), ex.getMessage());
               meterRegistry.counter("auth.errors", "type", "data_integrity").increment();

               // Extract constraint name if available
        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("constraint")) {
            message = "Resource already exists or violates constraints";
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ErrorCode.DUPLICATE_RESOURCE, message));
    }
}