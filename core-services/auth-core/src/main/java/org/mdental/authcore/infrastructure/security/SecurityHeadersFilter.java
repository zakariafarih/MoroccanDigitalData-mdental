package org.mdental.authcore.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds security headers to HTTP responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${mdental.security.hsts.max-age:31536000}")
    private long hstsMaxAgeSeconds;

    @Value("${mdental.security.hsts.include-subdomains:true}")
    private boolean hstsIncludeSubDomains;

    @Value("${mdental.security.hsts.preload:true}")
    private boolean hstsPreload;

    @Value("${mdental.security.csp.enabled:true}")
    private boolean cspEnabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Add HTTP Strict Transport Security header with configurable parameters
        StringBuilder hstsValue = new StringBuilder("max-age=").append(hstsMaxAgeSeconds);
        if (hstsIncludeSubDomains) {
            hstsValue.append("; includeSubDomains");
        }
        if (hstsPreload) {
            hstsValue.append("; preload");
        }
        response.setHeader("Strict-Transport-Security", hstsValue.toString());

        // Add Content Security Policy header with a reasonably restrictive policy
        if (cspEnabled) {
            response.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "font-src 'self' data:; " +
                            "img-src 'self' data:; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'; " +
                            "form-action 'self'; " +
                            "base-uri 'self'; " +
                            "object-src 'none'; " +
                            "media-src 'none'; " +
                            "worker-src 'self'; " +
                            "manifest-src 'self'");
        }

        // Add Cache-Control header to prevent caching of sensitive information
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Add other security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}