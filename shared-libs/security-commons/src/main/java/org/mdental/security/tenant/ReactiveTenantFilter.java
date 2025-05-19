package org.mdental.security.tenant;

import lombok.extern.slf4j.Slf4j;
import org.mdental.commons.constants.MdentalHeaders;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@Order(10) // High precedence to ensure it runs before security filters
public class ReactiveTenantFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String clinicIdHeader = exchange.getRequest().getHeaders().getFirst(MdentalHeaders.USER_CLINIC_ID);

        if (clinicIdHeader != null && !clinicIdHeader.isBlank()) {
            try {
                UUID clinicId = UUID.fromString(clinicIdHeader);
                log.debug("Setting tenant ID in reactive context: {}", clinicId);

                return chain.filter(exchange)
                        .contextWrite(context -> TenantContext.withTenantId(context, clinicId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid clinic ID format in header: {}", clinicIdHeader);
            }
        }

        return chain.filter(exchange);
    }
}