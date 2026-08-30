package com.leettrack.leettrack.service;

import com.leettrack.leettrack.entity.Problem;
import com.leettrack.leettrack.entity.SavedProblem;
import com.leettrack.leettrack.repository.ProblemRepository;
import com.leettrack.leettrack.repository.SavedProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-frequency interview topics weighted higher in revision sort.
 * This is a custom prioritization heuristic, not ML-based.
 * Resume framing: "custom revision queue with topic-weighted prioritization."
 */
@Service
public class SavedProblemService {

    private static final Map<String, Integer> TOPIC_WEIGHT = Map.of(
        "Array",   5,
        "DP",      5,
        "Graph",   5,
        "Tree",    4,
        "String",  4,
        "Hashing", 3,
        "Binary Search", 3
    );
    private static final int DEFAULT_WEIGHT = 1;

    private final SavedProblemRepository savedRepo;
    private final ProblemRepository problemRepo;
    private final TopicMapper topicMapper;

    public SavedProblemService(SavedProblemRepository savedRepo, ProblemRepository problemRepo, TopicMapper topicMapper) {
        this.savedRepo = savedRepo;
        this.problemRepo = problemRepo;
        this.topicMapper = topicMapper;
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    /**
     * Save a problem for a user. If the problem doesn't exist in our DB yet
     * (e.g. came from a live LeetCode/CF search), upsert it into Problem table first.
     */
    @Transactional
    public SavedProblem save(UUID userId, Map<String, Object> problemData) {
        // Upsert the problem into Problem table
        String platform   = (String) problemData.get("platform");
        String externalId = (String) problemData.get("externalId");

        Problem problem = problemRepo.findByPlatformAndExternalId(platform, externalId)
            .orElseGet(() -> {
                Problem p = new Problem();
                p.setTitle((String) problemData.get("title"));
                p.setPlatform(platform);
                p.setDifficulty((String) problemData.get("difficulty"));
                p.setTags((String) problemData.get("tags"));
                p.setCanonicalTopic(
                    problemData.containsKey("canonicalTopic")
                        ? (String) problemData.get("canonicalTopic")
                        : topicMapper.map((String) problemData.get("tags"))
                );
                p.setLink((String) problemData.get("link"));
                p.setExternalId(externalId);
                return problemRepo.save(p);
            });

        // Idempotent: if already saved, return existing
        return savedRepo.findByUserIdAndProblemId(userId, problem.getId())
            .orElseGet(() -> {
                SavedProblem sp = new SavedProblem();
                sp.setUserId(userId);
                sp.setProblem(problem);
                sp.setCanonicalTopicCache(problem.getCanonicalTopic());
                return savedRepo.save(sp);
            });
    }

    // ─── List (paginated, grouped by topic) ───────────────────────────────────

    /**
     * Returns saved problems grouped by canonicalTopic — frontend just renders
     * the map directly without needing to group itself (keeps JS dumb).
     */
    public Map<String, List<Map<String, Object>>> listGroupedByTopic(UUID userId) {
        List<SavedProblem> all = savedRepo.findAllByUserIdOrderedByTopic(userId);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();

        for (SavedProblem sp : all) {
            String topic = sp.getCanonicalTopicCache();
            grouped.computeIfAbsent(topic, k -> new ArrayList<>()).add(toMap(sp));
        }

        return grouped;
    }

    // ─── Patch (notes + revision toggle) ──────────────────────────────────────

    @Transactional
    public SavedProblem patch(UUID userId, Long savedProblemId, Boolean markRevision, String notes) {
        SavedProblem sp = savedRepo.findById(savedProblemId)
            .filter(s -> s.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (markRevision != null) {
            sp.setMarkedForRevision(markRevision);
            if (markRevision) sp.setLastRevisedAt(Instant.now());
        }
        if (notes != null) {
            sp.setNotes(notes);
        }

        return savedRepo.save(sp);
    }

    @Transactional
    public void delete(UUID userId, Long savedProblemId) {
        SavedProblem sp = savedRepo.findById(savedProblemId)
            .filter(s -> s.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        savedRepo.delete(sp);
    }

    // ─── Revision queue ────────────────────────────────────────────────────────

    /**
     * Returns problems marked for revision, sorted by a weighted score:
     *   score = daysSinceLastRevised * topicWeight
     * Higher score = should be reviewed first.
     *
     * This is a custom prioritization heuristic. High-frequency interview topics
     * (Array, DP, Graph, Tree) are weighted higher so they surface more often.
     */
    public List<Map<String, Object>> revisionQueue(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size * 3); // fetch more, sort, then trim
        Page<SavedProblem> marked = savedRepo.findRevisionQueue(userId, pageable);

        Instant now = Instant.now();

        return marked.stream()
            .sorted(Comparator.comparingDouble(sp -> -weightedScore(sp, now)))
            .limit(size)
            .map(this::toMap)
            .toList();
    }

    private double weightedScore(SavedProblem sp, Instant now) {
        long daysSince = sp.getLastRevisedAt() == null
            ? 365 // never revised → very stale
            : (now.getEpochSecond() - sp.getLastRevisedAt().getEpochSecond()) / 86400;
        int weight = TOPIC_WEIGHT.getOrDefault(sp.getCanonicalTopicCache(), DEFAULT_WEIGHT);
        return daysSince * weight;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public List<SavedProblem> findAllForExport(UUID userId) {
        return savedRepo.findAllByUserIdOrderedByTopic(userId);
    }

    private Map<String, Object> toMap(SavedProblem sp) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                sp.getId());
        m.put("savedAt",           sp.getSavedAt());
        m.put("markedForRevision", sp.isMarkedForRevision());
        m.put("lastRevisedAt",     sp.getLastRevisedAt());
        m.put("notes",             sp.getNotes());
        m.put("canonicalTopic",    sp.getCanonicalTopicCache());
        if (sp.getProblem() != null) {
            Problem p = sp.getProblem();
            m.put("problemId",   p.getId());
            m.put("title",       p.getTitle());
            m.put("platform",    p.getPlatform());
            m.put("difficulty",  p.getDifficulty());
            m.put("link",        p.getLink());
        }
        return m;
    }
}
