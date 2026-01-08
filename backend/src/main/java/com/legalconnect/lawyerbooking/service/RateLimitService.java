package com.legalconnect.lawyerbooking.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Separate buckets for different endpoint types
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Standard API bucket: 100 requests per minute
    private Bucket getStandardBucket() {
        return buckets.computeIfAbsent("standard", k -> Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build());
    }

    // Costly AI bucket: 5 requests per minute
    public Bucket getAiBucket() {
        return buckets.computeIfAbsent("ai", k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build());
    }

    public boolean tryConsumeAi() {
        return getAiBucket().tryConsume(1);
    }

    public boolean tryConsumeStandard() {
        return getStandardBucket().tryConsume(1);
    }
}
