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
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // High priority
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Pattern LOGIN_PATH_PATTERN = Pattern.compile("/auth/[^/]+/login");

    private final Cache<String, Bucket> ipRateLimiters = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100000)
            .build();

    private final Cache<String, Integer> failedAttempts = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(100000)
            .build();

    @Value("${mdental.auth.rate-limit.requests-per-minute:5}")
    private int requestsPerMinute;

    @Value("${mdental.auth.rate-limit.max-consecutive-failures:3}")
    private int maxConsecutiveFailures;

    @Value("${mdental.auth.rate-limit.lockout-minutes:15}")
    private int lockoutMinutes;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Only apply to login endpoints
        String path = request.getRequestURI();
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client IP
        String clientIp = getClientIp(request);

        // Check if this IP is already locked out due to consecutive failures
        Integer attempts = failedAttempts.getIfPresent(clientIp);
        if (attempts != null && attempts >= maxConsecutiveFailures) {
            log.warn("IP {} locked out due to too many failed attempts", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(lockoutMinutes * 60));
            response.getWriter().write("Too many failed login attempts. Please try again later.");
            return;
        }

        // Apply rate limiting
        Bucket bucket = ipRateLimiters.get(clientIp, new Function<String, Bucket>() {
            @Override
            public Bucket apply(String key) {
                return createBucket();
            }
        });

        if (bucket.tryConsume(1)) {
            // Rate limit not exceeded, wrap the response to track login failures
            HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response);

            try {
                filterChain.doFilter(request, wrappedResponse);

                // Check if login failed (401 status)
                if (wrappedResponse.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                    recordFailedAttempt(clientIp);
                } else {
                    // Reset counter on successful login
                    failedAttempts.invalidate(clientIp);
                }
            } finally {
                // Copy wrapped response content to the original response
                wrappedResponse.copyToResponse(response);
            }
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Rate limit exceeded. Please try again later.");
        }
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute,
                        Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") &&
                LOGIN_PATH_PATTERN.matcher(request.getRequestURI()).matches();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void recordFailedAttempt(String clientIp) {
        Integer attempts = failedAttempts.getIfPresent(clientIp);
        failedAttempts.put(clientIp, attempts == null ? 1 : attempts + 1);
    }

    // Helper class to wrap response and capture status
    private static class HttpServletResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private int status = 200;

        public HttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int status) {
            super.setStatus(status);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public void copyToResponse(HttpServletResponse response) {
            response.setStatus(status);
            // In a real implementation, copy headers and content too
        }
    }
}