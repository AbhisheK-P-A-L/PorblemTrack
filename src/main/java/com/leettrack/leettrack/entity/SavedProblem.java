package com.leettrack.leettrack.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's saved problem. userId is the Supabase UUID from the JWT's `sub` claim —
 * we don't maintain a separate users table; Supabase auth owns that.
 *
 * Indexes on userId ensure every list query is O(log n) even with many users.
 * This is the main scalability story: "all queries are indexed by userId."
 */
@Entity
@Table(
    name = "saved_problem",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_problem",
        columnNames = {"user_id", "problem_id"}
    ),
    indexes = {
        // Primary access pattern: "all saved problems for user X"
        @Index(name = "idx_saved_user_id", columnList = "user_id"),
        // Revision queue: "problems for user X sorted by last revised"
        @Index(name = "idx_saved_user_revision", columnList = "user_id, last_revised_at"),
        // Topic grouping: "user X's problems in topic Y"
        @Index(name = "idx_saved_user_topic", columnList = "user_id, canonical_topic_cache")
    }
)
public class SavedProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Supabase user UUID. NOT a FK to a users table in our DB —
     * Supabase Auth owns that. We just store the UUID string.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt = Instant.now();

    @Column(name = "marked_for_revision")
    private boolean markedForRevision = false;

    @Column(name = "last_revised_at")
    private Instant lastRevisedAt;

    /** Free-text notes the user adds to the problem */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Denormalized cache of problem's canonicalTopic — avoids a join when
     * grouping saved problems by topic. Kept in sync when saving.
     */
    @Column(name = "canonical_topic_cache")
    private String canonicalTopicCache;

    public SavedProblem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public boolean isMarkedForRevision() { return markedForRevision; }
    public void setMarkedForRevision(boolean markedForRevision) { this.markedForRevision = markedForRevision; }

    public Instant getLastRevisedAt() { return lastRevisedAt; }
    public void setLastRevisedAt(Instant lastRevisedAt) { this.lastRevisedAt = lastRevisedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCanonicalTopicCache() { return canonicalTopicCache; }
    public void setCanonicalTopicCache(String canonicalTopicCache) { this.canonicalTopicCache = canonicalTopicCache; }
}
