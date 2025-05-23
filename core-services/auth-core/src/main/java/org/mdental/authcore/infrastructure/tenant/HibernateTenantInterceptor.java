package org.mdental.authcore.infrastructure.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Hibernate interceptor that sets tenant filter parameter from TenantContext.
 */
@Component
@Slf4j
public class HibernateTenantInterceptor implements HibernatePropertiesCustomizer, PreLoadEventListener {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.session_factory.interceptor", this);
    }

    /**
     * Activate the tenant filter for the given session.
     *
     * @param session the Hibernate session
     */
    public void activateFilter(Session session) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            if (session.getEnabledFilter("tenantFilter") == null) {
                log.debug("Enabling tenant filter with ID: {}", tenantId);
                session.enableFilter("tenantFilter").setParameter("clinicId", tenantId);
            }
        }
    }

    /**
     * Called before loading an entity. This is where we activate the filter.
     *
     * @param event the pre-load event
     */
    @Override
    public void onPreLoad(PreLoadEvent event) {
        activateFilter(event.getSession());
    }
}