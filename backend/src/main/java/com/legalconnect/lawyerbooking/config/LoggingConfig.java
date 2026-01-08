package com.legalconnect.lawyerbooking.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * LOGGING CONFIGURATION
 * 
 * PURPOSE: Structured logging with correlation IDs
 * 
 * FEATURES:
 * - Request correlation IDs
 * - Structured log format
 * - Security event logging
 * - Performance metrics
 */
@Configuration
public class LoggingConfig {

    /**
     * Correlation ID filter for request tracking
     */
    @Bean
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                
                String correlationId = UUID.randomUUID().toString();
                MDC.put("correlationId", correlationId);
                
                // Add correlation ID to response header
                response.setHeader("X-Correlation-ID", correlationId);
                
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    MDC.remove("correlationId");
                }
            }
        };
    }

    /**
     * Performance logging filter
     */
    @Bean
    public OncePerRequestFilter performanceLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                
                long startTime = System.currentTimeMillis();
                
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    long duration = System.currentTimeMillis() - startTime;
                    
                    // Log slow requests (> 1 second)
                    if (duration > 1000) {
                        System.out.println(String.format(
                            "SLOW_REQUEST: %s %s - %dms - Correlation-ID: %s",
                            request.getMethod(),
                            request.getRequestURI(),
                            duration,
                            MDC.get("correlationId")
                        ));
                    }
                }
            }
        };
    }
}
