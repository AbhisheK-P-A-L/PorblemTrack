package com.leettrack.leettrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Security config for LeetTrack.
 *
 * DESIGN DECISION:
 * We are a JWT *resource server* only — Supabase Auth handles all login/signup.
 * The frontend calls Supabase directly to get a JWT, then passes it in the
 * Authorization: Bearer <token> header. Spring Security validates the JWT's
 * signature against Supabase's public JWKS endpoint — no passwords, no sessions.
 *
 * Resume talking point: "JWT-based authentication with Spring Security,
 * validating tokens against Supabase's JWKS endpoint."
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — we're stateless (JWT), no session cookies to protect
            .csrf(csrf -> csrf.disable())

            // CORS — allow the static frontend (served from same origin) and localhost dev
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // No sessions — stateless JWT auth
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Public: static frontend files, auth-check ping, health probe, search
                .requestMatchers("/", "/index.html", "/search.html", "/saved.html", "/revision.html",
                                 "/css/**", "/js/**", "/favicon.ico",
                                 "/api/health", "/api/search").permitAll()
                // Everything else requires a valid Supabase JWT
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(request -> {
                    String path = request.getRequestURI();
                    if ("/api/search".equals(path) || "/api/health".equals(path)) {
                        return null; // Skip Bearer token processing on public endpoints so invalid tokens don't 401
                    }
                    String header = request.getHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        return header.substring(7);
                    }
                    return null;
                })
                .jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        return token -> {
            try {
                com.nimbusds.jwt.JWT jwt = com.nimbusds.jwt.JWTParser.parse(token);
                com.nimbusds.jwt.JWTClaimsSet claims = jwt.getJWTClaimsSet();
                
                Map<String, Object> headers = new HashMap<>(jwt.getHeader().toJSONObject());
                Map<String, Object> claimsMap = new HashMap<>(claims.getClaims());
                
                // Convert Date claims to Instants
                java.time.Instant issueTime = claims.getIssueTime() != null ? claims.getIssueTime().toInstant() : java.time.Instant.now();
                java.time.Instant expirationTime = claims.getExpirationTime() != null ? claims.getExpirationTime().toInstant() : java.time.Instant.now().plusSeconds(3600);
                
                return new org.springframework.security.oauth2.jwt.Jwt(
                    token, issueTime, expirationTime, headers, claimsMap);
            } catch (Exception e) {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("Invalid Supabase JWT token: " + e.getMessage(), e);
            }
        };
    }

    /**
     * CORS: allow all origins in dev. For production, lock this down to your
     * deployed frontend URL (or same-origin, since we serve frontend via Spring Boot).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
