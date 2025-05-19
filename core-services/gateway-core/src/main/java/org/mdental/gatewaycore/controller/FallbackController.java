package org.mdental.gatewaycore.controller;

import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.constants.MdentalHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> serviceFallback(
            @PathVariable String service,
            ServerWebExchange exchange) {

        String requestId = exchange.getRequest().getHeaders()
                .getFirst(MdentalHeaders.GATEWAY_REQUEST_ID);

        if (requestId == null) {
            requestId = "unknown-" + System.currentTimeMillis();
        }

        log.warn("Fallback triggered for service: {}, requestId: {}", service, requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);

        Map<String, Object> error = new HashMap<>();
        error.put("code", "SERVICE_UNAVAILABLE");
        error.put("message", "Service \"" + service + "\" is unavailable");
        error.put("service", service);
        error.put("requestId", requestId);
        error.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        response.put("error", error);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}