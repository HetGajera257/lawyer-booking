package com.legalconnect.lawyerbooking.security;

import java.security.Principal;
import java.util.Objects;

/**
 * USER PRINCIPAL
 * 
 * PURPOSE: Custom principal object for JWT-based authentication
 * 
 * SECURITY FEATURES:
 * - Immutable user identity from JWT claims
 * - Contains userId, username, and userType
 * - Implements Principal interface for Spring Security compatibility
 * - Used consistently across HTTP and WebSocket authentication
 * 
 * DESIGN PRINCIPLES:
 * - userId and userType come ONLY from JWT (never from request body)
 * - Immutable after creation
 * - Simple, serializable object for security context
 */
public class UserPrincipal implements Principal {
    private final Long userId;
    private final String username;
    private final String userType;

    public UserPrincipal(Long userId, String username, String userType) {
        // Validate inputs
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username cannot be null or empty");
        }
        if (userType == null || userType.trim().isEmpty()) {
            throw new IllegalArgumentException("userType cannot be null or empty");
        }
        
        this.userId = userId;
        this.username = username.trim();
        this.userType = userType.toLowerCase().trim(); // Normalize to lowercase
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public String getName() {
        return username;
    }

    /**
     * Check if this user is a regular user
     */
    public boolean isUser() {
        return "user".equals(userType);
    }

    /**
     * Check if this user is a lawyer
     */
    public boolean isLawyer() {
        return "lawyer".equals(userType);
    }

    /**
     * Get the Spring Security role for this user
     */
    public String getRole() {
        return "ROLE_" + userType.toUpperCase();
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return userId.equals(that.userId) &&
                username.equals(that.username) &&
                userType.equals(that.userType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
