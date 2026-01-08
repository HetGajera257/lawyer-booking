package com.legalconnect.lawyerbooking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GLOBAL EXCEPTION HANDLER
 * 
 * PURPOSE: Centralized exception handling for production
 * 
 * FEATURES:
 * - Consistent error responses
 * - No stacktrace exposure
 * - Structured logging
 * - Security event logging
 * - Request correlation
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        logSecurityEvent("AUTHENTICATION_FAILED", request, ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.UNAUTHORIZED,
            "Authentication failed",
            "Please check your credentials and try again"
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle authorization exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        logSecurityEvent("ACCESS_DENIED", request, ex.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.FORBIDDEN,
            "Access denied",
            "You don't have permission to access this resource"
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            "Request validation failed"
        );
        errorResponse.put("errors", errors);
        
        logger.warn("Validation failed for request {}: {}", 
                   request.getDescription(false), errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Constraint violation",
            "Request constraints violated"
        );
        errorResponse.put("errors", errors);
        
        logger.warn("Constraint violation for request {}: {}", 
                   request.getDescription(false), errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle entity not found exceptions
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        logger.warn("Entity not found: {} for request: {}", 
                   ex.getMessage(), request.getDescription(false));
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Resource not found",
            "The requested resource was not found"
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {} for request: {}", 
                   ex.getMessage(), request.getDescription(false));
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Invalid request",
            ex.getMessage()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle resource not found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        
        logger.warn("No handler found for request: {}", request.getDescription(false));
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "Resource not found",
            "The requested resource was not found"
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error for request: {}", 
                    request.getDescription(false), ex);
        
        Map<String, Object> errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred. Please try again later."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        return errorResponse;
    }

    /**
     * Log security events
     */
    private void logSecurityEvent(String eventType, WebRequest request, String details) {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getRemoteAddr();
        }
        
        logger.warn("SECURITY_EVENT: {} | IP: {} | Path: {} | Details: {}", 
                   eventType, clientIP, request.getDescription(false), details);
    }
}
