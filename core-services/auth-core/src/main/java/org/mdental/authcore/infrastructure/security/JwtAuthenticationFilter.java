package org.mdental.authcore.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.domain.service.AuthService;
import org.mdental.commons.model.AuthPrincipal;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for JWT authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String COOKIE_ACCESS_TOKEN = "ACCESS_TOKEN";

    private final AuthService authService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Try to extract token from Authorization header
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            Optional<String> tokenFromHeader = extractTokenFromHeader(authHeader);

            // Try to extract token from cookie
            Optional<String> tokenFromCookie = extractTokenFromCookie(request);

            // Use the token from the header if present, otherwise use the token from the cookie
            Optional<String> token = tokenFromHeader.isPresent() ? tokenFromHeader : tokenFromCookie;

            token.ifPresent(t -> {
                try {
                    // Validate the token
                    AuthPrincipal principal = authService.validateToken(t);

                    // Set MDC values for logging
                    MDC.put("tenantId", principal.tenantId().toString());
                    MDC.put("userId", principal.id().toString());
                    MDC.put("username", principal.username());
                    MDC.put("requestId", request.getHeader("X-Request-ID"));

                    // Create authentication
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            AuthService.getAuthorities(principal.roles())
                    );

                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    log.debug("Invalid token: {}", e.getMessage());
                }
            });

            filterChain.doFilter(request, response);
        } finally {
            // Clear authentication after the request
            SecurityContextHolder.clearContext();
            MDC.clear();
        }
    }

    /**
     * Extract token from Authorization header.
     *
     * @param authHeader the Authorization header
     * @return the token or empty if not found
     */
    private Optional<String> extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    /**
     * Extract token from cookie.
     *
     * @param request the HTTP request
     * @return the token or empty if not found
     */
    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (COOKIE_ACCESS_TOKEN.equals(cookie.getName())) {
                    return Optional.ofNullable(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }
}