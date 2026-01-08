package com.legalconnect.lawyerbooking.filter;

import com.legalconnect.lawyerbooking.security.UserPrincipal;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT AUTHENTICATION FILTER
 * 
 * PURPOSE: Extract and validate JWT token, populate SecurityContext
 * 
 * EXECUTION FLOW:
 * 1. Extract JWT from Authorization header
 * 2. Validate JWT signature and expiration
 * 3. Extract userId, username, userType from JWT claims
 * 4. Create UserPrincipal with extracted data
 * 5. Create Authentication object with ROLE_USER or ROLE_LAWYER
 * 6. Set Authentication in SecurityContext
 * 7. Continue filter chain
 * 
 * SECURITY GUARANTEES:
 * - userId and role come ONLY from JWT (never from request body)
 * - Invalid/expired tokens are rejected silently
 * - SecurityContext is populated for downstream services
 * - Request attributes are set for backward compatibility
 * 
 * CRITICAL: This filter runs BEFORE any authorization checks
 * 
 * PRODUCTION CHANGES:
 * - Enhanced error handling
 * - Better logging for debugging
 * - Stricter JWT validation
 * - Clearer security context population
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain chain)
            throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        
        try {
            // Step 1: Extract JWT token from Authorization header
            String jwtToken = extractJwtFromRequest(request);
            
            if (jwtToken != null) {
                // Step 2: Validate and process token
                authenticateWithJwt(jwtToken, request, requestURI);
            } else {
                // No token present - request will be unauthenticated
                // Spring Security will handle this based on endpoint permissions
                logger.debug("No JWT token found in request to: {}", requestURI);
                
                // Clear security context to ensure no residual authentication
                SecurityContextHolder.clearContext();
            }
            
        } catch (Exception e) {
            // CRITICAL: Never fail the request due to JWT issues
            // Just log and continue - Spring Security will deny if authentication required
            logger.error("JWT authentication failed for request to {}: {}", 
                        requestURI, e.getMessage());
            
            // Clear any partial authentication
            SecurityContextHolder.clearContext();
        }

        // Step 3: Continue filter chain regardless of authentication result
        chain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * 
     * Expected format: "Bearer <token>"
     * 
     * @return JWT token string or null if not present/invalid format
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Validate JWT and populate SecurityContext
     * 
     * SECURITY: This is the ONLY place where user identity is established
     * All downstream services MUST read from SecurityContext, never from request body
     * 
     * PRODUCTION ENHANCEMENTS:
     * - Stricter validation logic
     * - Better error handling
     * - Clearer logging
     * - Consistent security context population
     * 
     * @param jwtToken JWT token string
     * @param request HTTP request (for setting attributes)
     * @param requestURI Request URI for logging
     */
    private void authenticateWithJwt(String jwtToken, HttpServletRequest request, String requestURI) {
        try {
            // Step 1: Extract claims from JWT
            String username = jwtUtil.extractUsername(jwtToken);
            String userType = jwtUtil.extractUserType(jwtToken);
            Long userId = jwtUtil.extractUserId(jwtToken);
            
            // Step 2: Validate required claims are present
            if (username == null || userType == null || userId == null) {
                logger.warn("JWT token missing required claims (username, userType, or userId) for request: {}", requestURI);
                return;
            }
            
            // Step 3: Validate userType format
            if (!userType.equalsIgnoreCase("user") && !userType.equalsIgnoreCase("lawyer")) {
                logger.warn("JWT token contains invalid userType: {} for user: {} on request: {}", userType, username, requestURI);
                return;
            }
            
            // Step 4: Check if already authenticated (avoid re-authentication)
            var existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null && existingAuth.isAuthenticated()) {
                logger.debug("SecurityContext already contains authentication, skipping JWT processing for: {}", requestURI);
                return;
            }
            
            // Step 5: Validate token signature and expiration
            if (!jwtUtil.validateToken(jwtToken, username)) {
                logger.warn("JWT token validation failed for user: {} on request: {}", username, requestURI);
                return;
            }
            
            // Step 6: Create UserPrincipal with JWT claims
            // This is the SINGLE SOURCE OF TRUTH for user identity
            UserPrincipal principal = new UserPrincipal(userId, username, userType);
            
            // Step 7: Create authority based on userType
            // userType should be "user" or "lawyer" (lowercase in JWT)
            // We convert to "ROLE_USER" or "ROLE_LAWYER" for Spring Security
            String role = "ROLE_" + userType.toUpperCase();
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
            
            // Step 8: Create Authentication object
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    principal,           // Principal (our custom UserPrincipal)
                    null,               // Credentials (not needed for JWT)
                    Collections.singletonList(authority)  // Authorities/Roles
                );
            
            // Step 9: Set request details (IP address, session ID, etc.)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Step 10: CRITICAL - Set authentication in SecurityContext
            // This makes the user "authenticated" for Spring Security
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Step 11: Set request attributes for backward compatibility
            // Some legacy code might read from request attributes
            // But new code should ALWAYS use SecurityContext
            request.setAttribute("userId", userId);
            request.setAttribute("userType", userType);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            
            logger.info("Successfully authenticated user: {} (ID: {}, Type: {}, Role: {}) for request: {}", 
                        username, userId, userType, role, requestURI);
            
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT token has expired for request {}: {}", requestURI, e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.warn("Malformed JWT token for request {}: {}", requestURI, e.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.warn("Invalid JWT signature for request {}: {}", requestURI, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during JWT authentication for request {}: {}", requestURI, e.getMessage(), e);
        }
    }

    /**
     * IMPORTANT: This filter runs for EVERY request
     * 
     * For public endpoints (/api/auth/**, /ws/**):
     * - Filter runs but finds no token
     * - SecurityContext remains empty
     * - Spring Security allows access (permitAll)
     * 
     * For protected endpoints (/api/**):
     * - Filter runs and validates token
     * - SecurityContext is populated
     * - Spring Security checks authentication
     * - If authenticated: allow access
     * - If not authenticated: return 401 Unauthorized
     */
}
