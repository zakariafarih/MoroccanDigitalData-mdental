package org.mdental.authcore.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Rate limiter implementation.
 */
@Component
@Slf4j
public class RateLimitConfiguration {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Get a bucket for a key with a specific rate limit.
     *
     * @param key the key
     * @param tokensPerMinute the tokens per minute
     * @return the bucket
     */
    public Bucket resolveBucket(String key, long tokensPerMinute) {
        return buckets.computeIfAbsent(key, k -> createBucket(tokensPerMinute));
    }

    /**
     * Get a bucket for a tenant and feature with a custom configuration.
     *
     * @param tenantId the tenant ID
     * @param feature the feature
     * @param tokensPerMinute the tokens per minute
     * @return the bucket
     */
    public Bucket resolveTenantFeatureBucket(String tenantId, String feature, long tokensPerMinute) {
        return buckets.computeIfAbsent(tenantId + ":" + feature, k -> createBucket(tokensPerMinute));
    }

    /**
     * Create a bucket with a specific rate limit.
     *
     * @param tokensPerMinute the tokens per minute
     * @return the bucket
     */
    private Bucket createBucket(long tokensPerMinute) {
        Bandwidth limit = Bandwidth.classic(tokensPerMinute,
                Refill.intervally(tokensPerMinute, Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}