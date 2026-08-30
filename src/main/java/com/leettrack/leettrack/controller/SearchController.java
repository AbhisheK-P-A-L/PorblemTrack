package com.leettrack.leettrack.controller;

import com.leettrack.leettrack.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/search?q=two+sum
     *
     * Searches LeetCode (GraphQL), Codeforces (REST), and GitHub seed dataset in DB.
     * All upstream calls happen server-side — frontend never touches external APIs.
     *
     * Returns: list of normalized problem objects with fields:
     *   title, platform, difficulty, tags, canonicalTopic, link, externalId
     *
     * FUTURE WORK: add ?platform=LEETCODE filter to narrow source.
     * FUTURE WORK: add pagination (?page=0&size=20) if result sets grow large.
     */
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam("q") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return searchService.search(keyword.trim());
    }
}
