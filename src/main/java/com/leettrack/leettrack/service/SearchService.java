package com.leettrack.leettrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leettrack.leettrack.entity.Problem;
import com.leettrack.leettrack.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Server-side search across three sources:
 *   1. LeetCode — unofficial GraphQL endpoint
 *   2. Codeforces — official public REST API
 *   3. GitHub seed dataset — queried from our Postgres DB
 *
 * Results are merged and normalized into a flat list of Problem-like maps.
 *
 * FUTURE WORK (Redis cache):
 *   @Cacheable(value = "searchResults", key = "#keyword")
 *   Slot in above the method signature once Spring Cache + Redis is wired up.
 *   TTL = 300 seconds (5 min). This alone removes ~95% of upstream API load
 *   for common queries like "two sum".
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final RestTemplate restTemplate;
    private final ProblemRepository problemRepository;
    private final TopicMapper topicMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SearchService(RestTemplate restTemplate, ProblemRepository problemRepository, TopicMapper topicMapper) {
        this.restTemplate = restTemplate;
        this.problemRepository = problemRepository;
        this.topicMapper = topicMapper;
    }

    public List<Map<String, Object>> search(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        results.addAll(searchLeetCode(keyword));
        results.addAll(searchCodeforces(keyword));
        results.addAll(searchDb(keyword));

        return results;
    }

    // ─── LeetCode ────────────────────────────────────────────────────────────

    /**
     * Uses LeetCode's unofficial GraphQL endpoint.
     * Endpoint: https://leetcode.com/graphql
     * No API key required for public problem search.
     *
     * NOTE: LeetCode may rate-limit or block this endpoint without notice.
     * If it breaks, remove this method and rely on the GitHub seed dataset.
     */
    private List<Map<String, Object>> searchLeetCode(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        String graphqlQuery = """
            {
              "query": "query problemsetQuestionList($categorySlug: String, $limit: Int, $skip: Int, $filters: QuestionListFilterInput) { problemsetQuestionList: questionList(categorySlug: $categorySlug, limit: $limit, skip: $skip, filters: $filters) { questions: data { difficulty frontendQuestionId: questionFrontendId title titleSlug topicTags { name slug } } } }",
              "variables": {
                "categorySlug": "",
                "skip": 0,
                "limit": 20,
                "filters": { "searchKeywords": "%s" }
              }
            }
            """.formatted(keyword.replace("\"", "\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Referer", "https://leetcode.com/problemset/");
        headers.set("User-Agent", "Mozilla/5.0");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                "https://leetcode.com/graphql",
                HttpMethod.POST,
                new HttpEntity<>(graphqlQuery, headers),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode questions = root
                .path("data")
                .path("problemsetQuestionList")
                .path("questions");

            if (questions.isArray()) {
                for (JsonNode q : questions) {
                    String slug = q.path("titleSlug").asText();
                    String rawTags = buildTagString(q.path("topicTags"));
                    Map<String, Object> p = new HashMap<>();
                    p.put("title",          q.path("title").asText());
                    p.put("platform",       "LEETCODE");
                    p.put("difficulty",     q.path("difficulty").asText());
                    p.put("tags",           rawTags);
                    p.put("canonicalTopic", topicMapper.map(rawTags));
                    p.put("link",           "https://leetcode.com/problems/" + slug + "/");
                    p.put("externalId",     slug);
                    results.add(p);
                }
            }
        } catch (Exception e) {
            log.warn("LeetCode search failed for '{}': {}", keyword, e.getMessage());
            // Degrade gracefully — return empty, other sources still work
        }

        return results;
    }

    private String buildTagString(JsonNode topicTags) {
        if (!topicTags.isArray()) return "";
        List<String> names = new ArrayList<>();
        for (JsonNode tag : topicTags) names.add(tag.path("name").asText());
        return String.join(",", names);
    }

    // ─── Codeforces ──────────────────────────────────────────────────────────

    /**
     * Codeforces official public API — no key needed.
     * Endpoint: https://codeforces.com/api/problemset.problems?tags=<keyword>
     *
     * Limitation: Codeforces tags must be exact (e.g. "dp", "graphs", "trees").
     * Keyword search by title isn't supported — we filter client-side after fetch.
     * We request up to 500 problems and filter by title on our side.
     */
    private List<Map<String, Object>> searchCodeforces(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            String url = "https://codeforces.com/api/problemset.problems";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"OK".equals(root.path("status").asText())) return results;

            JsonNode problems = root.path("result").path("problems");
            int count = 0;
            for (JsonNode p : problems) {
                if (count >= 30) break; // cap at 30 results per search
                String title = p.path("name").asText();
                String rawTags = buildCfTags(p.path("tags"));
                String canonicalTopic = topicMapper.map(rawTags);
                String kw = keyword.toLowerCase();

                boolean matches = title.toLowerCase().contains(kw) ||
                                  rawTags.toLowerCase().contains(kw) ||
                                  canonicalTopic.toLowerCase().contains(kw);
                if (!matches) continue;
                int rating = p.path("rating").asInt(0);
                int contestId = p.path("contestId").asInt();
                String index = p.path("index").asText();
                String externalId = contestId + index;

                Map<String, Object> result = new HashMap<>();
                result.put("title",          title);
                result.put("platform",       "CODEFORCES");
                result.put("difficulty",     ratingToDifficulty(rating));
                result.put("tags",           rawTags);
                result.put("canonicalTopic", topicMapper.map(rawTags));
                result.put("link",           "https://codeforces.com/problemset/problem/" + contestId + "/" + index);
                result.put("externalId",     externalId);
                results.add(result);
                count++;
            }
        } catch (Exception e) {
            log.warn("Codeforces search failed for '{}': {}", keyword, e.getMessage());
        }

        return results;
    }

    private String buildCfTags(JsonNode tags) {
        List<String> list = new ArrayList<>();
        if (tags.isArray()) tags.forEach(t -> list.add(t.asText()));
        return String.join(",", list);
    }

    private String ratingToDifficulty(int rating) {
        if (rating == 0)       return "Unknown";
        if (rating <= 1200)    return "Easy";
        if (rating <= 1800)    return "Medium";
        return "Hard";
    }

    // ─── DB (GitHub seed) ─────────────────────────────────────────────────────

    private List<Map<String, Object>> searchDb(String keyword) {
        return problemRepository
            .searchByKeyword(keyword, org.springframework.data.domain.PageRequest.of(0, 20))
            .stream()
            .map(this::problemToMap)
            .toList();
    }

    private Map<String, Object> problemToMap(Problem p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",             p.getId());
        m.put("title",          p.getTitle());
        m.put("platform",       p.getPlatform());
        m.put("difficulty",     p.getDifficulty());
        m.put("tags",           p.getTags());
        m.put("canonicalTopic", p.getCanonicalTopic());
        m.put("link",           p.getLink());
        m.put("externalId",     p.getExternalId());
        return m;
    }
}
