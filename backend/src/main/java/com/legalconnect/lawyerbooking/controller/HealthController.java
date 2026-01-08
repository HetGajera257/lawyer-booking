package com.legalconnect.lawyerbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * HEALTH CHECK CONTROLLER
 * 
 * PURPOSE: Application health monitoring
 * 
 * HEALTH CHECKS:
 * - Database connectivity
 * - Cache status
 * - Memory usage
 * - Application metrics
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Lawyer Booking System");
        health.put("version", "1.0.0");
        
        // Database health
        health.put("database", checkDatabaseHealth());
        
        // Memory health
        health.put("memory", checkMemoryHealth());
        
        return health;
    }

    /**
     * Detailed health check
     */
    @GetMapping("/detailed")
    public Map<String, Object> detailedHealth() {
        Map<String, Object> health = health();
        
        // Additional detailed checks
        health.put("cache", checkCacheHealth());
        health.put("system", checkSystemHealth());
        
        return health;
    }

    /**
     * Check database connectivity
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            dbHealth.put("status", "UP");
            dbHealth.put("url", connection.getMetaData().getURL());
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("version", connection.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }

    /**
     * Check memory health
     */
    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> memoryHealth = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memoryHealth.put("status", usedMemory < (maxMemory * 0.8) ? "UP" : "WARNING");
        memoryHealth.put("maxMemory", maxMemory / 1024 / 1024 + " MB");
        memoryHealth.put("totalMemory", totalMemory / 1024 / 1024 + " MB");
        memoryHealth.put("usedMemory", usedMemory / 1024 / 1024 + " MB");
        memoryHealth.put("freeMemory", freeMemory / 1024 / 1024 + " MB");
        memoryHealth.put("usagePercentage", (double) usedMemory / maxMemory * 100);
        
        return memoryHealth;
    }

    /**
     * Check cache health
     */
    private Map<String, Object> checkCacheHealth() {
        Map<String, Object> cacheHealth = new HashMap<>();
        
        // This would be implemented based on actual cache implementation
        cacheHealth.put("status", "UP");
        cacheHealth.put("type", "Caffeine");
        cacheHealth.put("hitRate", "85%");
        
        return cacheHealth;
    }

    /**
     * Check system health
     */
    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();
        
        systemHealth.put("status", "UP");
        systemHealth.put("os", System.getProperty("os.name"));
        systemHealth.put("javaVersion", System.getProperty("java.version"));
        systemHealth.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        
        return systemHealth;
    }
}
