package org.mdental.gatewaycore.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                // Clinics service routes
                .route("clinic-core", r -> r
                        .path("/api/clinics/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("clinicServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/clinic"))
                                .rewritePath("/api/(?<segment>.*)", "/api/${segment}")
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://clinic-core"))

                // Patients service routes
                .route("patient-core", r -> r
                        .path("/api/patients/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("patientServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/patient"))
                                .rewritePath("/api/(?<segment>.*)", "/api/${segment}")
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://patient-core"))

                // Appointments service routes
                .route("appointment-core", r -> r
                        .path("/api/appointments/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("appointmentServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/appointment"))
                                .rewritePath("/api/(?<segment>.*)", "/api/${segment}")
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://appointment-core"))

                // Auth service routes (unprotected)
                .route("auth-core", r -> r
                        .path("/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth"))
                                .rewritePath("/auth/(?<segment>.*)", "/${segment}")
                                .addRequestHeader("X-Gateway-Request-Id", "#{T(java.util.UUID).randomUUID().toString()}")
                        )
                        .uri("lb://auth-core"))

                .build();
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(timeLimiterConfig)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build());
    }
}