package org.mdental.security.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterInterceptorTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private HttpServletResponse response;

    @Test
    void preHandle_shouldActivateFilter_whenValidClinicIdHeader() {
        // Arrange
        UUID clinicId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-ClinicId", clinicId.toString());

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        TenantFilterInterceptor interceptor = new TenantFilterInterceptor(entityManager);

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isTrue();
        assertThat(TenantContext.getTenantId()).isEqualTo(clinicId);
        verify(session).enableFilter("tenantFilter");
        verify(filter).setParameter("clinicId", clinicId);
    }

    @Test
    void preHandle_shouldNotActivateFilter_whenInvalidClinicIdHeader() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-ClinicId", "invalid-uuid");

        TenantFilterInterceptor interceptor = new TenantFilterInterceptor(entityManager);

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isTrue();
        assertThat(TenantContext.getTenantId()).isNull();
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void preHandle_shouldNotActivateFilter_whenNoClinicIdHeader() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();

        TenantFilterInterceptor interceptor = new TenantFilterInterceptor(entityManager);

        // Act
        boolean result = interceptor.preHandle(request, response, null);

        // Assert
        assertThat(result).isTrue();
        assertThat(TenantContext.getTenantId()).isNull();
        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void afterCompletion_shouldClearTenantContext() {
        // Arrange
        UUID clinicId = UUID.randomUUID();
        TenantContext.setTenantId(clinicId);
        assertThat(TenantContext.getTenantId()).isEqualTo(clinicId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        TenantFilterInterceptor interceptor = new TenantFilterInterceptor(entityManager);

        // Act
        interceptor.afterCompletion(request, response, null, null);

        // Assert
        assertThat(TenantContext.getTenantId()).isNull();
    }
}