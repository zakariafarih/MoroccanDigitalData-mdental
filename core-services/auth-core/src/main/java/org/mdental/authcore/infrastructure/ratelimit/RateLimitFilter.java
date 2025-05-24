package org.mdental.authcore.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mdental.authcore.application.service.FeatureService;
import org.mdental.authcore.infrastructure.tenant.TenantContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for rate limiting requests.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private static final String RATE_LIMIT_FEATURE = "rate-limit";
    private static final String HEADER_LIMIT = "X-Rate-Limit-Limit";
    private static final String HEADER_REMAINING = "X-Rate-Limit-Remaining";
    private static final String HEADER_RESET = "X-Rate-Limit-Reset";

    private final RateLimitConfiguration rateLimiter;
    private final FeatureService featureService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // No tenant context, skip rate limiting
            filterChain.doFilter(request, response);
            return;
        }

        // Check if rate limiting is enabled for this tenant
        if (!featureService.isFeatureEnabled(tenantId, RATE_LIMIT_FEATURE)) {
            // Rate limiting disabled for this tenant
            filterChain.doFilter(request, response);
            return;
        }

        // Get rate limit configuration
        Map<String, Object> config = featureService.getFeatureConfig(tenantId, RATE_LIMIT_FEATURE);
        long tokensPerMinute = getTenantRateLimit(config);

        // Get bucket for this tenant
        Bucket bucket = rateLimiter.resolveTenantFeatureBucket(
                tenantId.toString(), RATE_LIMIT_FEATURE, tokensPerMinute);

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader(HEADER_LIMIT, String.valueOf(tokensPerMinute));
            response.addHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));
            response.addHeader(HEADER_RESET, String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));

            // Continue with the request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader(HEADER_LIMIT, String.valueOf(tokensPerMinute));
            response.addHeader(HEADER_REMAINING, "0");
            response.addHeader(HEADER_RESET, String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.getWriter().write("Rate limit exceeded. Please try again later.");
        }
    }

    /**
     * Get the rate limit for a tenant.
     *
     * @param config the feature configuration
     * @return the rate limit
     */
    private long getTenantRateLimit(Map<String, Object> config) {
        // Default to 60 requests per minute
        if (config == null || config.isEmpty()) {
            return 60;
        }

        // Try to get the rate limit from the configuration
        Object rateLimit = config.get("requests_per_minute");
        if (rateLimit instanceof Number) {
            return ((Number) rateLimit).longValue();
        }

        return 60;
    }
}