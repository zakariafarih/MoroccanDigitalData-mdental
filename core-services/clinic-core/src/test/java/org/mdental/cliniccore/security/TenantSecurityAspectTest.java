package org.mdental.cliniccore.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mdental.commons.model.ErrorCode;
import org.mdental.commons.model.Role;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSecurityAspectTest {

    @Mock
    private TenantGuard tenantGuard;

    @InjectMocks
    private TenantSecurityAspect aspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private SecurityContext originalContext;

    @BeforeEach
    void saveOriginalContext() {
        originalContext = SecurityContextHolder.getContext();
        SecurityContextHolder.clearContext();

        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @AfterEach
    void restoreOriginalContext() {
        SecurityContextHolder.setContext(originalContext);
    }

    @Test
    void enforceTenantSecurity_WithNoUUIDParameter_ShouldThrowException() throws Throwable {
        // Arrange
        // Setup method signature to return a method with @SameClinic
        Method testMethod = TestController.class.getDeclaredMethod("testMethod", String.class);
        when(methodSignature.getMethod()).thenReturn(testMethod);

        // Setup method parameters
        String[] parameterNames = new String[]{"stringParam"};
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        // Setup method args (no UUID)
        Object[] args = new Object[]{"not-a-uuid"};
        when(joinPoint.getArgs()).thenReturn(args);

        // Setup auth context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/test-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")),
                "username");
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        TenantSecurityAspect.TenantSecurityException exception = assertThrows(
                TenantSecurityAspect.TenantSecurityException.class,
                () -> aspect.enforceTenantSecurity(joinPoint)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertEquals("Clinic identifier not found", exception.getMessage());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void enforceTenantSecurity_WithNonMatchingClinic_ShouldThrowException() throws Throwable {
        // Arrange
        UUID clinicId = UUID.randomUUID();

        // Setup method signature to return a method with @SameClinic
        Method testMethod = TestController.class.getDeclaredMethod("testMethodWithUUID", UUID.class);
        when(methodSignature.getMethod()).thenReturn(testMethod);

        // Setup method parameters
        String[] parameterNames = new String[]{"clinicId"};
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        // Setup method args
        Object[] args = new Object[]{clinicId};
        when(joinPoint.getArgs()).thenReturn(args);

        // Setup auth context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/test-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")),
                "username");
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock tenant guard to return false (different clinics)
        when(tenantGuard.sameClinic(clinicId)).thenReturn(false);

        // Act & Assert
        TenantSecurityAspect.TenantSecurityException exception = assertThrows(
                TenantSecurityAspect.TenantSecurityException.class,
                () -> aspect.enforceTenantSecurity(joinPoint)
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    void enforceTenantSecurity_WithNonStrictModeAndNonMatchingClinic_ShouldReturnNull() throws Throwable {
        // Arrange
        UUID clinicId = UUID.randomUUID();

        // Setup method signature to return a method with @SameClinic(strict=false)
        Method testMethod = TestController.class.getDeclaredMethod("testMethodNonStrict", UUID.class);
        when(methodSignature.getMethod()).thenReturn(testMethod);

        // Setup method parameters
        String[] parameterNames = new String[]{"clinicId"};
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        // Setup method args
        Object[] args = new Object[]{clinicId};
        when(joinPoint.getArgs()).thenReturn(args);

        // Setup auth context
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/test-realm")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLINIC_ADMIN")),
                "username");
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock tenant guard to return false (different clinics)
        when(tenantGuard.sameClinic(clinicId)).thenReturn(false);

        // Act
        Object result = aspect.enforceTenantSecurity(joinPoint);

        // Assert
        assertNull(result, "Should return null for non-strict mode when tenant doesn't match");
        verify(joinPoint, never()).proceed();
    }

    @Test
    void enforceTenantSecurity_WithSuperAdmin_ShouldAllowAccess() throws Throwable {
        // Arrange
        UUID clinicId = UUID.randomUUID();

        // Setup method signature
        Method testMethod = TestController.class.getDeclaredMethod("testMethodWithUUID", UUID.class);
        when(methodSignature.getMethod()).thenReturn(testMethod);

        // Setup method parameters
        String[] parameterNames = new String[]{"clinicId"};
        when(methodSignature.getParameterNames()).thenReturn(parameterNames);

        // Setup method args
        Object[] args = new Object[]{clinicId};
        when(joinPoint.getArgs()).thenReturn(args);

        // Setup auth context with SUPER_ADMIN role
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://auth.mdental.org/realms/platform")
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(Role.SUPER_ADMIN.asSpringRole())),
                "admin");
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock the joinPoint.proceed() to return a value
        when(joinPoint.proceed()).thenReturn("success");

        // Act
        Object result = aspect.enforceTenantSecurity(joinPoint);

        // Assert
        assertEquals("success", result);
        verify(joinPoint).proceed(); // Verify that proceed was called
        verify(tenantGuard, never()).sameClinic(any()); // Verify tenant guard was not called
    }

    // Test helper class with annotated methods
    private static class TestController {
        @SameClinic
        public void testMethod(String stringParam) {
            // Method for testing
        }

        @SameClinic
        public String testMethodWithUUID(UUID clinicId) {
            return "success";
        }

        @SameClinic(strict = false)
        public String testMethodNonStrict(UUID clinicId) {
            return "success";
        }
    }
}