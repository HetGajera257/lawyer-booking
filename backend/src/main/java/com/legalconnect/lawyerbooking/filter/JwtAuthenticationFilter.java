package com.legalconnect.lawyerbooking.filter;

import com.legalconnect.lawyerbooking.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        String userType = null;
        Long userId = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwtToken);
                userType = jwtUtil.extractUserType(jwtToken);
                userId = jwtUtil.extractUserId(jwtToken);
            } catch (Exception e) {
                logger.error("JWT Token parsing failed: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtil.validateToken(jwtToken, username)) {
                    com.legalconnect.lawyerbooking.security.UserPrincipal principal = 
                        new com.legalconnect.lawyerbooking.security.UserPrincipal(userId, username, userType);
                    
                    if (logger.isInfoEnabled()) {
                        logger.info("JWT Filter: Authenticated user " + username + " (ID: " + userId + ") with type " + userType);
                    }

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userType.toUpperCase());
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, Collections.singletonList(authority));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    request.setAttribute("userId", userId);
                    request.setAttribute("userType", userType);
                } else {
                    logger.warn("JWT Filter: Token validation failed for user " + username);
                }
        }
        chain.doFilter(request, response);
    }
}

