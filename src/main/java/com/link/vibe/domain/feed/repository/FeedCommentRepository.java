package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.FeedComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    @Query("SELECT c FROM FeedComment c " +
           "WHERE c.feed.feedId = :feedId " +
           "AND c.parentComment IS NULL " +
           "AND (:cursor IS NULL OR c.commentId < :cursor) " +
           "ORDER BY c.commentId DESC")
    List<FeedComment> findTopLevelComments(@Param("feedId") Long feedId,
                                           @Param("cursor") Long cursor,
                                           Pageable pageable);

    List<FeedComment> findByParentCommentCommentIdOrderByCommentIdAsc(Long parentCommentId);

    long countByFeedFeedId(Long feedId);

    // 배치 카운트 (N+1 최적화)
    @Query("SELECT c.feed.feedId, COUNT(c) FROM FeedComment c " +
           "WHERE c.feed.feedId IN :feedIds GROUP BY c.feed.feedId")
    List<Object[]> countByFeedFeedIdIn(@Param("feedIds") List<Long> feedIds);
}
