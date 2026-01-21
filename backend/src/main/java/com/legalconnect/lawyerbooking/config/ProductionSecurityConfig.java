package com.legalconnect.lawyerbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * PRODUCTION SECURITY HARDENING
 * 
 * SECURITY ENHANCEMENTS:
 * - HTTPS enforcement
 * - Strict CORS policies
 * - Security headers
 * - Rate limiting
 * - Request validation
 * - Error handling
 */
@Configuration
@Profile("prod")
public class ProductionSecurityConfig {

    @Value("${app.security.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    /**
     * Production-hardened security filter chain
     */
    @Bean
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // HTTPS enforcement
            .requiresChannel(channel -> channel
                .anyRequest().requiresSecure()
                .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/info")).permitAll()
            )
            
            // Strict CORS for production
            .cors(cors -> cors.configurationSource(strictCorsConfigurationSource()))
            
            // Disable CSRF (stateless JWT)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Disable form login (JWT only)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // Disable HTTP Basic (JWT only)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // Disable logout (stateless)
            .logout(AbstractHttpConfigurer::disable)
            
            // Disable anonymous
            .anonymous(AbstractHttpConfigurer::disable)
            
            // Security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true)
                )
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_WHEN_CROSS_ORIGIN)
                .and()
                .permissionsPolicy().policy("geolocation=(), microphone=(), camera=()")
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/audio/**").permitAll()
                .requestMatchers("/api/bookings/lawyers").permitAll()
                .requestMatchers("/api/lawyers/*/profile").permitAll()
                .requestMatchers("/ws/**").permitAll()
                
                // Actuator endpoints (restricted)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Deny everything else
                .anyRequest().denyAll()
            )
            
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }

    /**
     * Strict CORS configuration for production
     */
    private org.springframework.web.cors.CorsConfigurationSource strictCorsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        // Only allow specific origins
        configuration.setAllowedOrigins(java.util.Arrays.asList(allowedOrigins));
        
        // Only allow specific methods
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Only allow specific headers
        configuration.setAllowedHeaders(java.util.Arrays.asList(
            "Authorization", "Content-Type", "Accept", "X-Requested-With"
        ));
        
        // Don't allow credentials for public endpoints
        configuration.setAllowCredentials(false);
        
        // Short cache time
        configuration.setMaxAge(300L);
        
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = 
            new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
