package org.mdental.security.tenant;

import org.springframework.core.NamedThreadLocal;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> TENANT_ID = new NamedThreadLocal<>("Tenant ID");
    private static final String TENANT_CONTEXT_KEY = "TENANT_ID";

    // For imperative code (MVC)
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }

    // For reactive code (WebFlux)
    public static Mono<UUID> getTenantIdReactive() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(TENANT_CONTEXT_KEY)) {
                return Mono.just(ctx.get(TENANT_CONTEXT_KEY));
            }
            return Mono.empty();
        });
    }

    public static Context withTenantId(Context context, UUID tenantId) {
        return context.put(TENANT_CONTEXT_KEY, tenantId);
    }

    public static Mono<Void> setTenantIdReactive(ServerWebExchange exchange, UUID tenantId) {
        return Mono.deferContextual(ctx ->
                Mono.just(exchange).contextWrite(context -> withTenantId(context, tenantId))
                        .then());
    }
}