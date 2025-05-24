package org.mdental.gatewaycore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Configuration
@Slf4j
public class LoggingConfig {

    @Bean
    @Order(-1)
    public WebFilter loggingFilter() {
        return (exchange, chain) -> {
            Instant startTime = Instant.now();
            String requestId = UUID.randomUUID().toString();
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            exchange.getAttributes().put("requestId", requestId);
            log.info("Request: [{}] {} {}", requestId, method, path);

            return chain.filter(exchange)
                    .doOnSuccess(v -> {
                        Duration duration = Duration.between(startTime, Instant.now());
                        int statusCode = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value()
                                : 0;
                        log.info("Response: [{}] {} {} - {} - {}ms",
                                requestId, method, path, statusCode, duration.toMillis());
                    })
                    .doOnError(err -> {
                        Duration duration = Duration.between(startTime, Instant.now());
                        log.error("Error: [{}] {} {} - Error - {}ms: {}",
                                requestId, method, path, duration.toMillis(), err.getMessage(), err);
                    });
        };
    }
}