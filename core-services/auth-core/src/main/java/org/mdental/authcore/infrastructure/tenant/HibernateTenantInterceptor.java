package org.mdental.authcore.infrastructure.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class HibernateTenantInterceptor
        implements HibernatePropertiesCustomizer, PreLoadEventListener {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        // register this as a PRE_LOAD listener
        hibernateProperties.put(
                "hibernate.event.pre-load",
                new PreLoadEventListener[]{ this }
        );
    }

    @Override
    public void onPreLoad(PreLoadEvent event) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = event.getSession();
            if (session.getEnabledFilter("tenantFilter") == null) {
                log.debug("Enabling tenant filter with ID: {}", tenantId);
                session.enableFilter("tenantFilter")
                        .setParameter("clinicId", tenantId);
            }
        }
    }
}
