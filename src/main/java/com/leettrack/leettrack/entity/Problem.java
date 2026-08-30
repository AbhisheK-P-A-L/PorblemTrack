package com.leettrack.leettrack.entity;

import jakarta.persistence.*;

/**
 * Normalized problem entity. LeetCode, Codeforces, and GitHub dataset problems
 * all map to this single table. The (platform, externalId) pair is unique —
 * prevents duplicates when re-seeding or re-searching.
 *
 * Canonical topics are one of the fixed set defined in CanonicalTopic enum.
 * Tags are stored as a raw comma-separated string (simple, no extra join table needed).
 */
@Entity
@Table(
    name = "problem",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_platform_external_id",
        columnNames = {"platform", "external_id"}
    ),
    indexes = {
        @Index(name = "idx_problem_canonical_topic", columnList = "canonical_topic"),
        @Index(name = "idx_problem_platform", columnList = "platform")
    }
)
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** "LEETCODE", "CODEFORCES", "GITHUB" */
    @Column(nullable = false)
    private String platform;

    /** "Easy", "Medium", "Hard" or Codeforces rating bucket */
    private String difficulty;

    /** Raw tags from the source platform, comma-separated, e.g. "array,two-pointers" */
    @Column(length = 1000)
    private String tags;

    /**
     * Mapped to one of the canonical topics (Array, DP, Graph, etc.)
     * Indexed for efficient "group by topic" queries.
     */
    @Column(name = "canonical_topic")
    private String canonicalTopic;

    /** Direct URL to the problem on the source platform */
    @Column(length = 500)
    private String link;

    /** Platform's own ID/slug — used for the unique constraint */
    @Column(name = "external_id")
    private String externalId;

    public Problem() {}

    public Problem(Long id, String title, String platform, String difficulty, String tags, String canonicalTopic, String link, String externalId) {
        this.id = id;
        this.title = title;
        this.platform = platform;
        this.difficulty = difficulty;
        this.tags = tags;
        this.canonicalTopic = canonicalTopic;
        this.link = link;
        this.externalId = externalId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCanonicalTopic() { return canonicalTopic; }
    public void setCanonicalTopic(String canonicalTopic) { this.canonicalTopic = canonicalTopic; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
