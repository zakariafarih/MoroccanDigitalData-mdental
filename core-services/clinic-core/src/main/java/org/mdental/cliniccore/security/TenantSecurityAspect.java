package org.mdental.cliniccore.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.mdental.commons.exception.BaseException;
import org.mdental.commons.model.ErrorCode;
import org.mdental.commons.model.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSecurityAspect {

    private final TenantGuard tenantGuard;

    /**
     * Intercepts methods annotated with @SameClinic and validates that the user
     * belongs to the clinic specified by the parameter.
     */
    @Around("@annotation(org.mdental.cliniccore.security.SameClinic) || @within(org.mdental.cliniccore.security.SameClinic)")
    public Object enforceTenantSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        // Skip for system admin role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().contains(
                new SimpleGrantedAuthority(Role.SUPER_ADMIN.asSpringRole()))) {
            return joinPoint.proceed();
        }

        // Get the method signature and parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = signature.getParameterNames();

        // Get the annotation to check if a specific param is specified
        SameClinic annotation = method.getAnnotation(SameClinic.class);
        if (annotation == null) {
            // Try class-level annotation
            annotation = method.getDeclaringClass().getAnnotation(SameClinic.class);
        }

        boolean strict = annotation != null && annotation.strict();
        String paramName = annotation != null ? annotation.param() : "";

        // Find the clinic ID
        UUID clinicId = null;

        // If a specific parameter name is provided, look for it
        if (paramName != null && !paramName.isEmpty()) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName) && args[i] instanceof UUID) {
                    clinicId = (UUID) args[i];
                    break;
                }
            }

            // If we didn't find the named parameter, that's an error
            if (clinicId == null) {
                log.error("Named parameter '{}' not found or not a UUID in method {}",
                        paramName, method.getName());
                throw new TenantSecurityException("Clinic identifier not found", ErrorCode.FORBIDDEN);
            }
        } else {
            // Otherwise use parameter with "clinicId" name
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].contains("clinicId") && args[i] instanceof UUID) {
                    clinicId = (UUID) args[i];
                    break;
                }
            }

            // If no "clinicId" parameter was found, use the first UUID
            if (clinicId == null) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof UUID) {
                        clinicId = (UUID) arg;
                        String name = parameterNames[i];
                        log.debug("Using parameter '{}' as clinic ID", name);
                        break;
                    }
                }
            }
        }

        if (clinicId == null) {
            log.error("No UUID parameter found for tenant security check in method {}",
                    method.getName());
            throw new TenantSecurityException("Clinic identifier not found", ErrorCode.FORBIDDEN);
        }

        // Check if the user belongs to this clinic
        boolean sameClinic = tenantGuard.sameClinic(clinicId);

        if (!sameClinic) {
            if (strict) {
                throw new TenantSecurityException("Access denied: You don't have access to this resource", ErrorCode.FORBIDDEN);
            } else {
                log.warn("User attempted to access resource from different tenant: {}", clinicId);
                return null;
            }
        }

        return joinPoint.proceed();
    }

    public static class TenantSecurityException extends BaseException {
        public TenantSecurityException(String message, ErrorCode errorCode) {
            super(message, errorCode);
        }

        /** convenience ctor for tests and strict=true cases */
        public TenantSecurityException(String message) {
            super(message, ErrorCode.FORBIDDEN);
        }
    }

}