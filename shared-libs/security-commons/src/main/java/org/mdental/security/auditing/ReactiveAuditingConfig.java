package org.mdental.security.auditing;

import lombok.RequiredArgsConstructor;
import org.mdental.commons.constants.MdentalHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class ReactiveAuditingConfig {

    @Bean
    public ReactiveAuditorAware<String> reactiveAuditorProvider() {
        return () -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey(ServerWebExchange.class)) {
                ServerWebExchange exchange = ctx.get(ServerWebExchange.class);
                String username = exchange.getRequest().getHeaders()
                        .getFirst(MdentalHeaders.USER_USERNAME);
                if (username != null && !username.isBlank()) {
                    return Mono.just(username);
                }
            }
            return Mono.just("system");
        });
    }
}