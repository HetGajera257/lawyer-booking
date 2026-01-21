package com.legalconnect.lawyerbooking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * REQUEST VALIDATION FILTER
 * 
 * PURPOSE: Validate incoming requests for security
 * 
 * VALIDATIONS:
 * - Request size limits
 * - SQL injection prevention
 * - XSS prevention
 * - Malicious pattern detection
 */
@Component
public class RequestValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationFilter.class);

    // Maximum request size (10MB)
    private static final long MAX_REQUEST_SIZE = 10 * 1024 * 1024;

    // SQL injection patterns
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|truncate)\\s+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(or|and)\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(or|and)\\s+'[^']*'\\s*=\\s*'[^']*'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(--|;|\\/\\*|\\*\\/)", Pattern.CASE_INSENSITIVE)
    };

    // XSS patterns
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE)
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Validate request size
        if (request.getContentLengthLong() > MAX_REQUEST_SIZE) {
            logger.warn("Request too large: {} bytes from {}", 
                       request.getContentLengthLong(), getClientIP(request));
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large");
            return;
        }

        // Validate request parameters
        if (containsMaliciousPatterns(request)) {
            logger.warn("Malicious request detected from {}", getClientIP(request));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if request contains malicious patterns
     */
    private boolean containsMaliciousPatterns(HttpServletRequest request) {
        // Check parameters
        request.getParameterMap().forEach((key, values) -> {
            for (String value : values) {
                if (containsSqlInjection(value) || containsXss(value)) {
                    throw new SecurityException("Malicious pattern detected");
                }
            }
        });

        // Check headers
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && (containsSqlInjection(headerValue) || containsXss(headerValue))) {
                throw new SecurityException("Malicious pattern detected in header");
            }
        });

        return false;
    }

    /**
     * Check for SQL injection patterns
     */
    private boolean containsSqlInjection(String input) {
        if (input == null) return false;
        
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for XSS patterns
     */
    private boolean containsXss(String input) {
        if (input == null) return false;
        
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
