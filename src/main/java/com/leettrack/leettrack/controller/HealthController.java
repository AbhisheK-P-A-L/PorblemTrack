package com.leettrack.leettrack.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    /** Public — no auth needed. For Render/Railway health probes. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "app", "LeetTrack");
    }

    /**
     * Protected test endpoint — use this to verify JWT validation works.
     * Call with: curl -H "Authorization: Bearer <supabase-jwt>" http://localhost:8080/api/me
     * Should return your Supabase user ID and email from the JWT claims.
     */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "userId", jwt.getSubject(),          // Supabase user UUID (sub claim)
            "email",  jwt.getClaimAsString("email"),
            "claims", jwt.getClaims()             // all claims — remove in prod
        );
    }
}
