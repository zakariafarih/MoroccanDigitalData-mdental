package org.mdental.authcore.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds MDC context for logging.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract or generate request ID
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
                // Add request ID to response for correlation
                response.setHeader("X-Request-ID", requestId);
            }

            // Set MDC values
            MDC.put("requestId", requestId);
            MDC.put("ip", getClientIp(request));
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            // Extract tenant from path if possible
            String path = request.getRequestURI();
            if (path.startsWith("/auth/")) {
                String[] parts = path.split("/");
                if (parts.length > 2) {
                    MDC.put("tenant", parts[2]);
                }
            }

            // Continue with the filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Clear MDC context
            MDC.clear();
        }
    }

    /**
     * Extract client IP from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}