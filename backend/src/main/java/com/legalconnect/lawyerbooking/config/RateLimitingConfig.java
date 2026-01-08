package com.legalconnect.lawyerbooking.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RATE LIMITING CONFIGURATION
 * 
 * PURPOSE: Rate limiting for API endpoints
 * 
 * RATE LIMITS:
 * - General API: 100 requests per minute
 * - Authentication: 20 requests per minute
 * - File upload: 10 requests per minute
 * - WebSocket: 1000 messages per hour
 */
@Configuration
public class RateLimitingConfig {

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${rate-limit.requests-per-hour:1000}")
    private int requestsPerHour;

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * General API rate limiter
     */
    @Bean
    public Bucket apiRateLimiter() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Authentication rate limiter (stricter)
     */
    @Bean
    public Bucket authRateLimiter() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * File upload rate limiter
     */
    @Bean
    public Bucket fileUploadRateLimiter() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * WebSocket message rate limiter
     */
    @Bean
    public Bucket webSocketRateLimiter() {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofHours(1))))
                .build();
    }

    /**
     * Get rate limiter by type
     */
    public Bucket getRateLimiter(String type) {
        return bucketCache.computeIfAbsent(type, key -> {
            switch (key) {
                case "api":
                    return apiRateLimiter();
                case "auth":
                    return authRateLimiter();
                case "upload":
                    return fileUploadRateLimiter();
                case "websocket":
                    return webSocketRateLimiter();
                default:
                    return apiRateLimiter();
            }
        });
    }

    /**
     * Check if request is allowed
     */
    public boolean isAllowed(String type, String identifier) {
        Bucket bucket = getRateLimiter(type);
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining tokens
     */
    public long getRemainingTokens(String type) {
        Bucket bucket = getRateLimiter(type);
        return bucket.getAvailableTokens();
    }
}
