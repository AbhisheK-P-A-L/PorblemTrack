package com.leettrack.leettrack.controller;

import com.leettrack.leettrack.service.SavedProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved")
public class SavedProblemController {

    private final SavedProblemService service;

    public SavedProblemController(SavedProblemService service) {
        this.service = service;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Extract Supabase user UUID from the validated JWT `sub` claim. */
    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    // ── POST /api/saved ───────────────────────────────────────────────────────

    /**
     * Save a problem. Body: the problem object returned by the search endpoint.
     * Idempotent: re-saving an already-saved problem returns the existing record.
     *
     * Example body:
     * {
     *   "title": "Two Sum", "platform": "LEETCODE", "difficulty": "Easy",
     *   "tags": "Array,Hash Table", "canonicalTopic": "Hashing",
     *   "link": "https://leetcode.com/problems/two-sum/",
     *   "externalId": "two-sum"
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> save(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> problemData) {

        var saved = service.save(userId(jwt), problemData);
        return ResponseEntity.ok(Map.of(
            "id",      saved.getId(),
            "message", "Problem saved"
        ));
    }

    // ── GET /api/saved ────────────────────────────────────────────────────────

    /**
     * Returns saved problems grouped by canonicalTopic.
     * Response shape: { "Array": [...], "DP": [...], ... }
     * Frontend just iterates the keys — no client-side grouping needed.
     */
    @GetMapping
    public Map<String, List<Map<String, Object>>> listGrouped(
            @AuthenticationPrincipal Jwt jwt) {
        return service.listGroupedByTopic(userId(jwt));
    }

    // ── PATCH /api/saved/{id} ─────────────────────────────────────────────────

    /**
     * Update notes or revision flag on a saved problem.
     * Body (all fields optional):
     * { "markedForRevision": true, "notes": "Remember to handle edge case X" }
     *
     * Only the authenticated user can patch their own records (enforced in service).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, String>> patch(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Boolean markRevision = body.containsKey("markedForRevision")
            ? (Boolean) body.get("markedForRevision") : null;
        String notes = (String) body.get("notes");

        service.patch(userId(jwt), id, markRevision, notes);
        return ResponseEntity.ok(Map.of("message", "Updated"));
    }

    // ── DELETE /api/saved/{id} ────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        service.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/saved/revision ───────────────────────────────────────────────

    /**
     * Revision queue: problems marked for revision, sorted by weighted score
     * (daysSinceLastRevised × topicWeight). Highest score = review first.
     *
     * Custom prioritization heuristic — Array, DP, Graph, Tree weighted higher.
     * Resume: "custom revision queue with topic-weighted prioritization."
     */
    @GetMapping("/revision")
    public List<Map<String, Object>> revisionQueue(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.revisionQueue(userId(jwt), page, size);
    }
}
