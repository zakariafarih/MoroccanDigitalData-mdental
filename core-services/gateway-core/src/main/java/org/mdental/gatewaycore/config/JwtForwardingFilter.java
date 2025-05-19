package org.mdental.gatewaycore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtForwardingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> extractAndForwardJwtClaims(exchange, jwt))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange extractAndForwardJwtClaims(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        String username = jwt.getClaimAsString("preferred_username");
        if (username != null) {
            requestBuilder.header("X-User-Username", username);
        }

        String clinicId = jwt.getClaimAsString("clinicId");
        if (clinicId != null) {
            requestBuilder.header("X-User-ClinicId", clinicId);
        }

        String email = jwt.getClaimAsString("email");
        if (email != null) {
            requestBuilder.header("X-User-Email", email);
        }

        requestBuilder.header("X-User-Subject", jwt.getSubject());
        requestBuilder.header("X-User-Issuer", jwt.getIssuer().toString());

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    @Override
    public int getOrder() {
        return -99; // Just after authentication
    }
}
