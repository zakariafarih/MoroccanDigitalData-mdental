package org.mdental.authcore.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting filter specifically for password reset endpoints.
 */
@Component
@Order(1) // High priority
@Slf4j
public class PasswordResetRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern PASSWORD_RESET_PATH_PATTERN =
            Pattern.compile("/auth/[^/]+/(forgot|reset)");

    private final Cache<String, Bucket> ipRateLimiters = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Value("${mdental.auth.rate-limit.password-reset-per-minute:3}")
    private int requestsPerMinute;

    @Value("${mdental.auth.rate-limit.password-reset-window-minutes:15}")
    private int rateLimitWindowMinutes;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only apply to password reset endpoints
        if (!isPasswordResetRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP
        String clientIp = getClientIp(request);

        // Get or create rate limiter for this IP
        Bucket bucket = ipRateLimiters.get(clientIp, key -> createBucket());

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Token consumed successfully, proceed with the request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Password reset rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(rateLimitWindowMinutes * 60));
            response.getWriter().write("Too many password reset attempts. Please try again later.");
        }
    }

    /**
     * Create a rate limiting bucket with the configured parameters.
     *
     * @return the configured bucket
     */
    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute,
                        Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Check if this is a password reset request.
     *
     * @param request the HTTP request
     * @return true if this is a password reset request
     */
    private boolean isPasswordResetRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") &&
                PASSWORD_RESET_PATH_PATTERN.matcher(request.getRequestURI()).matches();
    }

    /**
     * Extract client IP from the request.
     *
     * @param request the HTTP request
     * @return the client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}