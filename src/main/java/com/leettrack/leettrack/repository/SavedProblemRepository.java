package com.leettrack.leettrack.repository;

import com.leettrack.leettrack.entity.SavedProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedProblemRepository extends JpaRepository<SavedProblem, Long> {

    /** All saved problems for a user — paginated (scalability point) */
    Page<SavedProblem> findByUserId(UUID userId, Pageable pageable);

    /** Check if user already saved this problem */
    Optional<SavedProblem> findByUserIdAndProblemId(UUID userId, Long problemId);

    /** Problems marked for revision, ordered by last revised (oldest first) for spaced repetition */
    @Query("""
        SELECT sp FROM SavedProblem sp
        WHERE sp.userId = :userId
          AND sp.markedForRevision = true
        ORDER BY sp.lastRevisedAt ASC NULLS FIRST
        """)
    Page<SavedProblem> findRevisionQueue(@Param("userId") UUID userId, Pageable pageable);

    /**
     * All saved problems grouped by topic for the PDF export / saved page.
     * Returns all (no pagination) because PDF needs the full set.
     * This is safe because a user's saved list is bounded in practice.
     */
    @Query("""
        SELECT sp FROM SavedProblem sp
        JOIN FETCH sp.problem p
        WHERE sp.userId = :userId
        ORDER BY sp.canonicalTopicCache ASC, p.title ASC
        """)
    List<SavedProblem> findAllByUserIdOrderedByTopic(@Param("userId") UUID userId);
}
