package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    @Query("SELECT f FROM Feed f " +
           "WHERE f.isPublic = true " +
           "AND (:cursor IS NULL OR f.feedId < :cursor) " +
           "ORDER BY f.feedId DESC")
    List<Feed> findPublicFeeds(@Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT f FROM Feed f " +
           "WHERE f.user.userId = :userId " +
           "AND (:cursor IS NULL OR f.feedId < :cursor) " +
           "ORDER BY f.feedId DESC")
    List<Feed> findByUserId(@Param("userId") Long userId,
                            @Param("cursor") Long cursor,
                            Pageable pageable);

    boolean existsByUserUserIdAndVibeResultResultId(Long userId, Long resultId);
}
