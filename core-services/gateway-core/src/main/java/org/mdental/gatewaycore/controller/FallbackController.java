package org.mdental.gatewaycore.controller;

import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.model.ApiError;
import org.mdental.commons.model.ApiResponse;
import org.mdental.commons.model.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/{service}")
    public ResponseEntity<ApiResponse<Void>> serviceFallback(@PathVariable String service) {
        log.warn("Fallback triggered for service: {}", service);

        ApiError error = ApiError.builder()
                .code(ErrorCode.GENERAL_ERROR)
                .message(String.format("Service '%s' is currently unavailable. Please try again later.", service))
                .build();

        ApiResponse<Void> response = ApiResponse.error(error);

        return ResponseEntity.status(ErrorCode.GENERAL_ERROR.toHttpStatus()).body(response);
    }
}