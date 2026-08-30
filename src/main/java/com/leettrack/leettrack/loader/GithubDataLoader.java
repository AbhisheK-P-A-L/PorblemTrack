package com.leettrack.leettrack.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leettrack.leettrack.entity.Problem;
import com.leettrack.leettrack.repository.ProblemRepository;
import com.leettrack.leettrack.service.TopicMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Seeds the Problem table from a static JSON dataset bundled in the JAR.
 * Runs once at startup; skips existing records (idempotent via findByPlatformAndExternalId).
 *
 * Data source: /src/main/resources/data/github_problems.json
 * Format: JSON array of { "title": "...", "difficulty": "...", "tags": "...", "link": "..." }
 *
 * To populate this file, download from a curated repo such as:
 *   https://github.com/huihut/interview (or similar) and convert to the format above.
 *
 * We ship a small hardcoded fallback set of ~30 classic problems so the app works
 * immediately without a separate data prep step.
 */
@Component
public class GithubDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GithubDataLoader.class);

    private final ProblemRepository problemRepo;
    private final TopicMapper topicMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GithubDataLoader(ProblemRepository problemRepo, TopicMapper topicMapper) {
        this.problemRepo = problemRepo;
        this.topicMapper = topicMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Running GitHub dataset seed loader...");

        // Try to load from bundled JSON file first
        try (InputStream is = getClass().getResourceAsStream("/data/github_problems.json")) {
            if (is != null) {
                JsonNode arr = objectMapper.readTree(is);
                int loaded = 0;
                for (JsonNode node : arr) {
                    loaded += upsert(
                        node.path("title").asText(),
                        node.path("difficulty").asText("Medium"),
                        node.path("tags").asText(""),
                        node.path("link").asText(""),
                        toSlug(node.path("title").asText())
                    );
                }
                log.info("GitHub seed: {} problems loaded from JSON file", loaded);
                return;
            }
        } catch (Exception e) {
            log.warn("Could not load github_problems.json, using built-in fallback: {}", e.getMessage());
        }

        // Fallback: hardcoded classic interview problems
        loadFallback();
    }

    private void loadFallback() {
        Object[][] problems = {
            {"Two Sum",                        "Easy",   "Array,Hash Table",           "https://leetcode.com/problems/two-sum/"},
            {"Best Time to Buy and Sell Stock","Easy",   "Array,Dynamic Programming",  "https://leetcode.com/problems/best-time-to-buy-and-sell-stock/"},
            {"Valid Parentheses",              "Easy",   "String,Stack",               "https://leetcode.com/problems/valid-parentheses/"},
            {"Merge Two Sorted Lists",         "Easy",   "Linked List,Recursion",      "https://leetcode.com/problems/merge-two-sorted-lists/"},
            {"Maximum Subarray",               "Medium", "Array,Dynamic Programming",  "https://leetcode.com/problems/maximum-subarray/"},
            {"Climbing Stairs",                "Easy",   "Dynamic Programming,Math",   "https://leetcode.com/problems/climbing-stairs/"},
            {"Binary Tree Inorder Traversal",  "Easy",   "Tree,Binary Tree,DFS",       "https://leetcode.com/problems/binary-tree-inorder-traversal/"},
            {"Number of Islands",              "Medium", "Graph,BFS,DFS",              "https://leetcode.com/problems/number-of-islands/"},
            {"Longest Substring Without Repeating Characters", "Medium", "String,Sliding Window,Hashing", "https://leetcode.com/problems/longest-substring-without-repeating-characters/"},
            {"3Sum",                           "Medium", "Array,Two Pointers",         "https://leetcode.com/problems/3sum/"},
            {"Product of Array Except Self",   "Medium", "Array,Prefix Sum",           "https://leetcode.com/problems/product-of-array-except-self/"},
            {"Find Minimum in Rotated Sorted Array", "Medium", "Array,Binary Search",  "https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/"},
            {"Search in Rotated Sorted Array", "Medium", "Array,Binary Search",        "https://leetcode.com/problems/search-in-rotated-sorted-array/"},
            {"Container With Most Water",      "Medium", "Array,Two Pointers,Greedy",  "https://leetcode.com/problems/container-with-most-water/"},
            {"Coin Change",                    "Medium", "Dynamic Programming,BFS",    "https://leetcode.com/problems/coin-change/"},
            {"Longest Common Subsequence",     "Medium", "Dynamic Programming,String", "https://leetcode.com/problems/longest-common-subsequence/"},
            {"Word Break",                     "Medium", "Dynamic Programming,Trie",   "https://leetcode.com/problems/word-break/"},
            {"Combination Sum",                "Medium", "Array,Backtracking",         "https://leetcode.com/problems/combination-sum/"},
            {"Permutations",                   "Medium", "Array,Backtracking",         "https://leetcode.com/problems/permutations/"},
            {"Merge k Sorted Lists",           "Hard",   "Linked List,Heap",           "https://leetcode.com/problems/merge-k-sorted-lists/"},
            {"Top K Frequent Elements",        "Medium", "Array,Heap,Hashing",         "https://leetcode.com/problems/top-k-frequent-elements/"},
            {"Find Median from Data Stream",   "Hard",   "Heap,Design",                "https://leetcode.com/problems/find-median-from-data-stream/"},
            {"Serialize and Deserialize BST",  "Medium", "Tree,BST,DFS",               "https://leetcode.com/problems/serialize-and-deserialize-bst/"},
            {"Implement Trie",                 "Medium", "Trie,Design",                "https://leetcode.com/problems/implement-trie-prefix-tree/"},
            {"Course Schedule",                "Medium", "Graph,Topological Sort,BFS", "https://leetcode.com/problems/course-schedule/"},
            {"Clone Graph",                    "Medium", "Graph,DFS,BFS",              "https://leetcode.com/problems/clone-graph/"},
            {"Longest Palindromic Substring",  "Medium", "String,Dynamic Programming", "https://leetcode.com/problems/longest-palindromic-substring/"},
            {"Missing Number",                 "Easy",   "Array,Bit Manipulation,Math","https://leetcode.com/problems/missing-number/"},
            {"Reverse Linked List",            "Easy",   "Linked List,Recursion",      "https://leetcode.com/problems/reverse-linked-list/"},
            {"Validate Binary Search Tree",    "Medium", "Tree,BST,DFS",               "https://leetcode.com/problems/validate-binary-search-tree/"},
        };

        int loaded = 0;
        for (Object[] row : problems) {
            loaded += upsert((String) row[0], (String) row[1], (String) row[2],
                             (String) row[3], toSlug((String) row[0]));
        }
        log.info("GitHub seed fallback: {} classic problems loaded", loaded);
    }

    private int upsert(String title, String difficulty, String tags, String link, String externalId) {
        if (problemRepo.findByPlatformAndExternalId("GITHUB", externalId).isPresent()) {
            return 0; // skip duplicate
        }
        Problem p = new Problem();
        p.setTitle(title);
        p.setPlatform("GITHUB");
        p.setDifficulty(difficulty);
        p.setTags(tags);
        p.setCanonicalTopic(topicMapper.map(tags));
        p.setLink(link.isBlank() ? null : link);
        p.setExternalId(externalId);
        problemRepo.save(p);
        return 1;
    }

    private String toSlug(String title) {
        return title.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
    }
}
