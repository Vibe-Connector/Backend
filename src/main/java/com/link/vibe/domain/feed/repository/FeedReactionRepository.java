package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    @Query("SELECT fr.reactionType, COUNT(fr) FROM FeedReaction fr " +
           "WHERE fr.feed.feedId = :feedId GROUP BY fr.reactionType")
    List<Object[]> countByFeedIdGroupByReactionType(@Param("feedId") Long feedId);

    // 단일 반응 제약: 한 유저가 한 피드에 하나의 반응만 가능
    Optional<FeedReaction> findByFeedFeedIdAndUserUserId(Long feedId, Long userId);

    // 피드의 모든 반응 사용자 목록 (프로필 표시용)
    @Query("SELECT fr FROM FeedReaction fr " +
           "JOIN FETCH fr.user " +
           "WHERE fr.feed.feedId = :feedId " +
           "ORDER BY fr.createdAt DESC")
    List<FeedReaction> findAllWithUserByFeedId(@Param("feedId") Long feedId);

    // ── 배치 쿼리 (N+1 최적화) ──

    @Query("SELECT fr.feed.feedId, fr.reactionType, COUNT(fr) FROM FeedReaction fr " +
           "WHERE fr.feed.feedId IN :feedIds GROUP BY fr.feed.feedId, fr.reactionType")
    List<Object[]> countByFeedIdsGroupByReactionType(@Param("feedIds") List<Long> feedIds);

    @Query("SELECT fr FROM FeedReaction fr " +
           "WHERE fr.feed.feedId IN :feedIds AND fr.user.userId = :userId")
    List<FeedReaction> findByFeedFeedIdInAndUserUserId(@Param("feedIds") List<Long> feedIds,
                                                       @Param("userId") Long userId);
}
