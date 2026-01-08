package com.legalconnect.lawyerbooking.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * PRODUCTION-READY CACHING CONFIGURATION
 * 
 * CACHING STRATEGY:
 * - CACHE: Lawyer search results, lawyer profiles, case metadata
 * - DO NOT CACHE: Chat messages, appointments, case status, user data
 * 
 * CACHE EVICTION:
 * - Lawyer profile updates → evict profile cache
 * - Rating changes → evict search results
 * - Case assignments → evict case metadata
 * 
 * PERFORMANCE TARGETS:
 * - Lawyer search: 5 minutes TTL
 * - Lawyer profiles: 10 minutes TTL
 * - Case metadata: 15 minutes TTL
 * - Cache hit ratio: >80%
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine Cache Manager (fallback when Redis not available)
     * 
     * PERFORMANCE: In-memory caching with LRU eviction
     * MEMORY: Limited to prevent OOM
     * TTL: Configured per cache type
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Lawyer search cache (5 minutes TTL)
        cacheManager.registerCustomCache("lawyerSearch", buildLawyerSearchCache());
        
        // Lawyer profile cache (10 minutes TTL)
        cacheManager.registerCustomCache("lawyerProfiles", buildLawyerProfileCache());
        
        // Case metadata cache (15 minutes TTL)
        cacheManager.registerCustomCache("caseMetadata", buildCaseMetadataCache());
        
        // System configuration cache (30 minutes TTL)
        cacheManager.registerCustomCache("systemConfig", buildSystemConfigCache());
        
        return cacheManager;
    }

    /**
     * Lawyer search cache configuration
     * 
     * USAGE: Search results with pagination
     * TTL: 5 minutes (balance freshness vs performance)
     * SIZE: 1000 entries (prevent memory issues)
     */
    private Caffeine<Object, Object> buildLawyerSearchCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Lawyer profile cache configuration
     * 
     * USAGE: Individual lawyer profiles
     * TTL: 10 minutes (profiles change less frequently)
     * SIZE: 5000 entries (support many lawyers)
     */
    private Caffeine<Object, Object> buildLawyerProfileCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Case metadata cache configuration
     * 
     * USAGE: Case titles, categories, basic info
     * TTL: 15 minutes (case metadata relatively stable)
     * SIZE: 2000 entries (support active cases)
     */
    private Caffeine<Object, Object> buildCaseMetadataCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * System configuration cache
     * 
     * USAGE: System settings, configuration data
     * TTL: 30 minutes (config changes infrequent)
     * SIZE: 100 entries (small config data)
     */
    private Caffeine<Object, Object> buildSystemConfigCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Cache statistics monitoring
     * 
     * USAGE: Monitor cache performance and hit ratios
     * METRICS: Hit rate, miss rate, eviction count
     */
    @Bean
    public CacheStatistics cacheStatistics() {
        return new CacheStatistics();
    }

    /**
     * Cache statistics bean for monitoring
     */
    public static class CacheStatistics {
        public double getLawyerSearchHitRate() {
            // Implementation would depend on cache manager type
            return 0.0; // Placeholder
        }
        
        public double getLawyerProfileHitRate() {
            return 0.0; // Placeholder
        }
        
        public long getEvictionCount() {
            return 0L; // Placeholder
        }
    }
}
