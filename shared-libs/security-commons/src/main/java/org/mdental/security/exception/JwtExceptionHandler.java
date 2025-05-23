package org.mdental.security.exception;

import org.mdental.commons.model.ApiError;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.mdental.security.jwt.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handler for JWT exceptions that maps them to appropriate error codes
 */
@Slf4j
@RestControllerAdvice
@Order(1)
@RequiredArgsConstructor
public class JwtExceptionHandler {
    private final ObjectMapper objectMapper;

    /**
     * Maps JWT exceptions to HTTP responses with appropriate error codes
     *
     * @param ex The JWT exception
     * @param response The HTTP response
     */
    public void handleJwtException(JwtException ex, HttpServletResponse response) throws IOException {
        ErrorCode errorCode;

        if (ex instanceof TokenExpiredException) {
            errorCode = ErrorCode.JWT_EXPIRED;
        } else if (ex instanceof TokenSignatureInvalidException) {
            errorCode = ErrorCode.JWT_SIGNATURE_INVALID;
        } else {
            errorCode = ErrorCode.JWT_INVALID;
        }

        log.debug("JWT error: {}", ex.getMessage());

        ApiError error = ApiError.builder()
                .code(errorCode)
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> apiResponse = ApiResponse.error(error);

        response.setStatus(errorCode.toHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}