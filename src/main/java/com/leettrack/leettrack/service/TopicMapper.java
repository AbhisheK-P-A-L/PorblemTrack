package com.leettrack.leettrack.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Maps raw platform tags (from LeetCode, Codeforces, GitHub dataset) to one of
 * the canonical topics defined for LeetTrack.
 *
 * CANONICAL TOPICS:
 * Array, String, Linked List, Stack, Queue, Tree, Binary Tree, BST, Graph,
 * DP, Trie, Greedy, Backtracking, Binary Search, Two Pointers, Sliding Window,
 * Heap, Hashing, Bit Manipulation, Math, Recursion
 */
@Component
public class TopicMapper {

    // Map: canonical topic → list of raw tags that map to it (case-insensitive prefix match)
    private static final Map<String, List<String>> TAG_MAP = Map.ofEntries(
        Map.entry("Array",           List.of("array", "arrays")),
        Map.entry("String",          List.of("string", "strings", "string manipulation")),
        Map.entry("Linked List",     List.of("linked-list", "linked list", "linkedlist")),
        Map.entry("Stack",           List.of("stack", "monotonic stack")),
        Map.entry("Queue",           List.of("queue", "deque", "monotonic queue")),
        Map.entry("Binary Tree",     List.of("binary tree", "binary-tree")),
        Map.entry("BST",             List.of("binary search tree", "bst")),
        Map.entry("Tree",            List.of("tree", "n-ary tree", "segment tree", "binary indexed tree", "fenwick")),
        Map.entry("Graph",           List.of("graph", "bfs", "dfs", "breadth-first", "depth-first",
                                              "topological sort", "union find", "shortest path", "dijkstra")),
        Map.entry("DP",              List.of("dynamic programming", "dp", "memoization", "knapsack")),
        Map.entry("Trie",            List.of("trie", "prefix tree")),
        Map.entry("Greedy",          List.of("greedy")),
        Map.entry("Backtracking",    List.of("backtracking", "recursion backtracking")),
        Map.entry("Binary Search",   List.of("binary search", "binary-search")),
        Map.entry("Two Pointers",    List.of("two pointers", "two-pointers")),
        Map.entry("Sliding Window",  List.of("sliding window", "sliding-window")),
        Map.entry("Heap",            List.of("heap", "priority queue", "priority-queue")),
        Map.entry("Hashing",         List.of("hash table", "hash map", "hashing", "hash-table")),
        Map.entry("Bit Manipulation",List.of("bit manipulation", "bit-manipulation", "bitwise")),
        Map.entry("Math",            List.of("math", "number theory", "combinatorics", "geometry")),
        Map.entry("Recursion",       List.of("recursion", "divide and conquer", "divide-and-conquer"))
    );

    /**
     * Given a comma-separated string of raw tags, return the best canonical topic.
     * Falls back to "Array" (most common interview topic) if nothing matches.
     */
    public String map(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return "Array";

        String lower = rawTags.toLowerCase();

        for (Map.Entry<String, List<String>> entry : TAG_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }

        return "Array"; // safe default
    }
}
