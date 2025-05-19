package org.mdental.gatewaycore.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class TracingConfig {

    @Bean
    public WebFilter traceIdPropagationWebFilter(Tracer tracer, Propagator propagator) {
        return (exchange, chain) -> {
            String requestId = exchange.getRequest().getHeaders()
                    .getFirst("X-Gateway-Request-Id");

            if (requestId != null && !exchange.getResponse().getHeaders().containsKey("X-Trace-Id")) {
                exchange.getResponse().getHeaders().add("X-Trace-Id", tracer.currentSpan().context().traceId());
            }

            return chain.filter(exchange);
        };
    }
}