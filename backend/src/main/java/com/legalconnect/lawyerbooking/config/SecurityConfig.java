package com.legalconnect.lawyerbooking.config;

import com.legalconnect.lawyerbooking.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * PRODUCTION-READY SPRING SECURITY CONFIGURATION
 * 
 * PURPOSE: Enforce JWT-only authentication with ZERO default security mechanisms
 * 
 * KEY SECURITY PRINCIPLES:
 * 1. NO UserDetailsService - prevents in-memory user creation
 * 2. DISABLED AuthenticationManager bean - prevents default password generation
 * 3. NO formLogin - prevents login page creation
 * 4. NO httpBasic - prevents basic auth popup
 * 5. NO sessions - completely stateless
 * 6. JWT is the ONLY source of truth for identity
 * 
 * ARCHITECTURE:
 * - JwtAuthenticationFilter extracts userId + role from JWT
 * - SecurityContext is populated with UserPrincipal
 * - All services read from SecurityContext (never from request body)
 * - WebSocket handshake bypasses JWT (authenticated in STOMP layer)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * CORS Configuration
     * Allows frontend to communicate with backend from different origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins (update for production)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001", 
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001"
        ));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Allow all headers (including Authorization with JWT)
        configuration.setAllowedHeaders(List.of("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * MAIN SECURITY FILTER CHAIN
     * 
     * CRITICAL SECURITY CHANGES:
     * 1. Explicitly disable ALL default Spring Security mechanisms
     * 2. NO UserDetailsService bean = NO in-memory users
     * 3. NO AuthenticationManager bean = NO default password generation
     * 4. JWT filter runs BEFORE any Spring Security filters
     * 5. SecurityContext populated ONLY from JWT claims
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with our configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CRITICAL: Disable CSRF - not needed for stateless JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // CRITICAL: Explicitly disable form login
            // Prevents Spring Security from creating /login endpoint
            // Prevents default login page generation
            .formLogin(AbstractHttpConfigurer::disable)
            
            // CRITICAL: Explicitly disable HTTP Basic authentication
            // Prevents browser basic auth popup
            // Prevents WWW-Authenticate header
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // CRITICAL: Disable logout endpoint
            // We're stateless - no server-side session to invalidate
            .logout(AbstractHttpConfigurer::disable)
            
            // CRITICAL: Disable anonymous authentication
            // Forces all requests to be either authenticated or explicitly permitted
            .anonymous(AbstractHttpConfigurer::disable)
            
            // CRITICAL: Disable request cache
            // Prevents Spring Security from saving unauthorized requests
            .requestCache(AbstractHttpConfigurer::disable)
            
            // Authorization rules - order matters (most specific first)
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()
                
                // Public endpoints for audio processing
                .requestMatchers("/api/audio/**").permitAll()
                
                // Public lawyer browsing
                .requestMatchers("/api/bookings/lawyers").permitAll()
                .requestMatchers("/api/lawyers/*/profile").permitAll()
                
                // WebSocket handshake (JWT validated in STOMP interceptor)
                .requestMatchers("/ws/**").permitAll()
                
                // Public health check endpoints
                .requestMatchers("/api/public/**").permitAll()
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Deny everything else by default
                .anyRequest().denyAll()
            )
            
            // CRITICAL: Stateless session management
            // NEVER create HttpSession
            // NEVER use JSESSIONID cookie
            // SecurityContext is cleared after each request
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // CRITICAL: Add JWT filter BEFORE Spring Security's authentication filter
            // This ensures JWT is processed first and SecurityContext is populated
            // before any authorization checks
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * CRITICAL: WHY WE DON'T EXPOSE AuthenticationManager BEAN
     * 
     * Spring Boot auto-configuration behavior:
     * 1. If AuthenticationManager bean exists → Used for formLogin/httpBasic
     * 2. If UserDetailsService bean exists → Used for authentication
     * 3. If neither exists AND formLogin/httpBasic enabled → Generates default user
     * 
     * By NOT exposing these beans and disabling formLogin/httpBasic:
     * - Spring Security has NO authentication mechanism to configure
     * - NO default user is created
     * - NO random password appears in logs
     * - JWT filter becomes the ONLY authentication source
     * 
     * This is the KEY to eliminating default Spring Security behavior
     */
    
    /**
     * CRITICAL: WHY THIS SECURITY SETUP FIXES CHAT & AUTHORIZATION ISSUES
     * 
     * 1. JWT-ONLY AUTHENTICATION:
     *    - No default users = No identity confusion
     *    - JWT filter runs first = SecurityContext populated correctly
     *    - Role comes ONLY from JWT = No role spoofing
     * 
     * 2. RELIABLE SECURITYCONTEXT:
     *    - Filter runs before authorization checks
     *    - UserPrincipal contains userId + role from JWT
     *    - All services read from SecurityContext (never request body)
     * 
     * 3. WEBSOCKET COMPATIBILITY:
     *    - WebSocket handshake permitted (/ws/**)
     *    - JWT validation in STOMP interceptor
     *    - Same UserPrincipal used across HTTP + WebSocket
     * 
     * 4. NO IDENTITY SPOOFING:
     *    - userId and role extracted ONLY from JWT
     *    - JWT signed with secret key
     *    - No request body parameters for authentication
     * 
     * RESULT: Production-ready, JWT-only authentication with zero default security
     */
}
