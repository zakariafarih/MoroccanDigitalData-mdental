package org.mdental.authcore.infrastructure.tenant;

import java.util.UUID;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit extension that sets up the tenant context for tests.
 */
public class WithTenantExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestMethod()
                .flatMap(method -> {
                    WithTenant annotation = method.getAnnotation(WithTenant.class);
                    if (annotation != null) {
                        return java.util.Optional.of(annotation.value());
                    }

                    return context.getTestClass()
                            .map(clazz -> clazz.getAnnotation(WithTenant.class))
                            .map(WithTenant::value);
                })
                .ifPresent(tenantId -> TenantContext.setTenantId(UUID.fromString(tenantId)));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TenantContext.clear();
    }
}