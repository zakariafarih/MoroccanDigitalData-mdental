package org.mdental.gatewaycore.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder routes) {
        return routes.routes()

                .route("clinic-core-dev",
                        r -> r.path("/internal/dev/clinics/**")
                                .filters(f -> f
                                        .addRequestHeader("X-Gateway-Request-Id",
                                                "#{T(java.util.UUID).randomUUID().toString()}"))
                                .uri("lb://clinic-core"))

                // Clinic-core: all /api/clinics/**
                .route("clinic-core-api",
                        r -> r.path("/api/clinics/**")
                                .filters(f -> f
                                        .circuitBreaker(cb -> cb
                                                .setName("clinicServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/clinic"))
                                        .addRequestHeader("X-Gateway-Request-Id",
                                                "#{T(java.util.UUID).randomUUID().toString()}")
                                        .rewritePath("/api/clinics/(?<cid>[^/]+)(?<segment>/.*)", "/api/clinics/${cid}${segment}"))
                                .uri("lb://clinic-core"))

                // Patient-core: only nested under clinic
                .route("patient-core-api",
                        r -> r.path("/api/clinics/*/patients/**")
                                .filters(f -> f
                                        .circuitBreaker(cb -> cb
                                                .setName("patientServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/patient"))
                                        .addRequestHeader("X-Gateway-Request-Id",
                                                "#{T(java.util.UUID).randomUUID().toString()}")
                                        .rewritePath("/api/clinics/(?<cid>[^/]+)/patients(?<segment>/.*)", "/api/clinics/${cid}/patients${segment}"))
                                .uri("lb://patient-core"))

                // Appointment-core ( still not implemented )
                .route("appointment-core-api",
                        r -> r.path("/api/clinics/*/patients/*/appointments/**")
                                .filters(f -> f
                                        .circuitBreaker(cb -> cb
                                                .setName("appointmentServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/appointment"))
                                        .addRequestHeader("X-Gateway-Request-Id",
                                                "#{T(java.util.UUID).randomUUID().toString()}")
                                        .rewritePath("/api/clinics/(?<cid>[^/]+)/patients/(?<pid>[^/]+)/appointments(?<segment>/.*)",
                                                "/api/clinics/${cid}/patients/${pid}/appointments${segment}"))
                                .uri("lb://appointment-core"))

                // Auth-core (public)
                .route("auth-core-api",
                        r -> r.path("/auth/**")
                                .filters(f -> f
                                        .circuitBreaker(cb -> cb
                                                .setName("authServiceCircuitBreaker")
                                                .setFallbackUri("forward:/fallback/auth"))
                                        .addRequestHeader("X-Gateway-Request-Id",
                                                "#{T(java.util.UUID).randomUUID().toString()}"))
                                .uri("lb://auth-core"))

                .build();
    }
}