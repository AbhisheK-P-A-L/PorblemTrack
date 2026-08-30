package com.leettrack.leettrack.repository;

import com.leettrack.leettrack.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /** Used by seed loader to skip duplicates */
    Optional<Problem> findByPlatformAndExternalId(String platform, String externalId);

    /** Search problems stored in DB (GitHub seed + previously cached) by title keyword */
    Page<Problem> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    /** Narrow DB search to a specific platform matching title, tags, or canonicalTopic */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Problem p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(p.tags) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(p.canonicalTopic) LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<Problem> searchByKeyword(@org.springframework.data.repository.query.Param("kw") String kw, Pageable pageable);
}
