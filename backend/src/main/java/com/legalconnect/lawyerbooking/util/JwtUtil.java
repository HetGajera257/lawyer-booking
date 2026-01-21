package com.legalconnect.lawyerbooking.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT UTILITY CLASS
 * 
 * PURPOSE: Generate, validate, and extract claims from JWT tokens
 * 
 * SECURITY FEATURES:
 * - HMAC-SHA256 signing algorithm
 * - Configurable secret key and expiration
 * - Comprehensive token validation
 * - Claim extraction utilities
 * 
 * PRODUCTION ENHANCEMENTS:
 * - Stronger validation logic
 * - Better error handling
 * - Secret key validation
 * - Token expiration handling
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key-should-be-at-least-256-bits-long-for-HS256-algorithm}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    /**
     * Get signing key for JWT token
     * Ensures secret key is long enough for HS256 algorithm
     */
    private SecretKey getSigningKey() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long for HS256 algorithm");
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extract username (subject) from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract userId from JWT token custom claim
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /**
     * Extract userType from JWT token custom claim
     */
    public String extractUserType(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userType", String.class);
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extraction utility
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and validate JWT token, extract all claims
     * Enhanced with better error handling
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new io.jsonwebtoken.security.SignatureException("Invalid JWT signature", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new io.jsonwebtoken.MalformedJwtException("Malformed JWT token", e);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new io.jsonwebtoken.ExpiredJwtException(token, null, "JWT token has expired", e);
        } catch (Exception e) {
            throw new io.jsonwebtoken.JwtException("Failed to parse JWT token", e);
        }
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            // If we can't extract expiration, consider token expired
            return true;
        }
    }

    /**
     * Generate JWT token with user claims
     * 
     * @param userId User's unique identifier
     * @param username User's username (will be subject)
     * @param userType User's type ("user" or "lawyer")
     * @return JWT token string
     */
    public String generateToken(Long userId, String username, String userType) {
        // Validate inputs
        if (userId == null || username == null || userType == null) {
            throw new IllegalArgumentException("userId, username, and userType cannot be null");
        }
        
        if (!userType.equalsIgnoreCase("user") && !userType.equalsIgnoreCase("lawyer")) {
            throw new IllegalArgumentException("userType must be 'user' or 'lawyer'");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", userType.toLowerCase()); // Store in lowercase for consistency
        
        return createToken(claims, username);
    }

    /**
     * Create JWT token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate JWT token against username
     * Enhanced validation with better error handling
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            
            // Check if username matches
            if (!tokenUsername.equals(username)) {
                return false;
            }
            
            // Check if token is expired
            return !isTokenExpired(token);
            
        } catch (Exception e) {
            // Any exception during validation means token is invalid
            return false;
        }
    }

    /**
     * Validate JWT token (without username check)
     * Used for WebSocket authentication where username is extracted from token
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token expiration time in milliseconds
     */
    public Long getExpiration() {
        return expiration;
    }

    /**
     * Check if secret key is properly configured
     * Useful for startup validation
     */
    public boolean isSecretKeyValid() {
        return secret != null && secret.length() >= 32;
    }
}

