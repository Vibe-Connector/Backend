package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    List<Feed> findByVibeResultResultIdIn(java.util.Collection<Long> resultIds);

    // ── @EntityGraph (User, VibeResult 즉시 로딩) — B+D 전략 ──

    @EntityGraph(attributePaths = {"user", "vibeResult"})
    @Query("SELECT f FROM Feed f " +
           "WHERE f.isPublic = true " +
           "AND (:cursor IS NULL OR f.feedId < :cursor) " +
           "ORDER BY f.feedId DESC")
    List<Feed> findPublicFeedsWithFetch(@Param("cursor") Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "vibeResult"})
    @Query("SELECT f FROM Feed f " +
           "WHERE f.user.userId = :userId " +
           "AND (:cursor IS NULL OR f.feedId < :cursor) " +
           "ORDER BY f.feedId DESC")
    List<Feed> findByUserIdWithFetch(@Param("userId") Long userId,
                                     @Param("cursor") Long cursor,
                                     Pageable pageable);

    // ── Strategy C: Native JOIN (단일 쿼리로 모든 데이터 집계) ──

    @Query(value =
        "SELECT f.feed_id, f.caption, f.is_public, f.view_count, f.created_at, f.updated_at, " +
        "       u.user_id, u.nickname, u.profile_image_url, " +
        "       vr.result_id, vr.generated_image_url, vr.phrase, " +
        "       COALESCE(c.comment_cnt, 0) AS comment_count, " +
        "       COALESCE(r.like_cnt, 0) AS like_count, " +
        "       COALESCE(r.dislike_cnt, 0) AS dislike_count, " +
        "       COALESCE(r.wow_cnt, 0) AS wow_count, " +
        "       COALESCE(r.love_cnt, 0) AS love_count, " +
        "       CAST(my.reaction_type AS VARCHAR) AS my_reaction " +
        "FROM feeds f " +
        "JOIN users u ON u.user_id = f.user_id " +
        "JOIN vibe_results vr ON vr.result_id = f.result_id " +
        "LEFT JOIN (" +
        "    SELECT feed_id, " +
        "        SUM(CASE WHEN reaction_type = 'LIKE' THEN 1 ELSE 0 END) AS like_cnt, " +
        "        SUM(CASE WHEN reaction_type = 'DISLIKE' THEN 1 ELSE 0 END) AS dislike_cnt, " +
        "        SUM(CASE WHEN reaction_type = 'WOW' THEN 1 ELSE 0 END) AS wow_cnt, " +
        "        SUM(CASE WHEN reaction_type = 'LOVE' THEN 1 ELSE 0 END) AS love_cnt " +
        "    FROM feed_reactions GROUP BY feed_id" +
        ") r ON r.feed_id = f.feed_id " +
        "LEFT JOIN (" +
        "    SELECT feed_id, COUNT(*) AS comment_cnt " +
        "    FROM feed_comments WHERE deleted_at IS NULL GROUP BY feed_id" +
        ") c ON c.feed_id = f.feed_id " +
        "LEFT JOIN feed_reactions my ON my.feed_id = f.feed_id AND my.user_id = :currentUserId " +
        "WHERE f.is_public = true AND f.deleted_at IS NULL " +
        "AND (:cursor IS NULL OR f.feed_id < CAST(:cursor AS BIGINT)) " +
        "ORDER BY f.feed_id DESC",
        nativeQuery = true)
    List<Object[]> findPublicFeedsWithJoin(
        @Param("cursor") Long cursor,
        @Param("currentUserId") Long currentUserId,
        Pageable pageable);

    @Query(value =
        "SELECT f.feed_id, f.caption, f.view_count, f.created_at, " +
        "       u.user_id AS author_id, u.nickname AS author_nickname, u.profile_image_url, " +
        "       vr.generated_image_url, vr.result_id, " +
        "       COALESCE(r.reaction_cnt, 0) AS reaction_count, " +
        "       COALESCE(c.comment_cnt, 0) AS comment_count, " +
        "       (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 10 " +
        "         + f.view_count " +
        "         + (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 1000 " +
        "           / (f.view_count + 10) AS popularity_score " +
        "FROM feeds f " +
        "JOIN users u ON u.user_id = f.user_id " +
        "JOIN vibe_results vr ON vr.result_id = f.result_id " +
        "LEFT JOIN (" +
        "    SELECT feed_id, COUNT(*) AS reaction_cnt " +
        "    FROM feed_reactions GROUP BY feed_id" +
        ") r ON r.feed_id = f.feed_id " +
        "LEFT JOIN (" +
        "    SELECT fc.feed_id, COUNT(*) AS comment_cnt " +
        "    FROM feed_comments fc " +
        "    JOIN feeds ff ON ff.feed_id = fc.feed_id " +
        "    WHERE fc.user_id <> ff.user_id AND fc.deleted_at IS NULL " +
        "    GROUP BY fc.feed_id" +
        ") c ON c.feed_id = f.feed_id " +
        "WHERE f.is_public = true " +
        "  AND f.deleted_at IS NULL " +
        "  AND f.created_at >= :since " +
        "  AND (" +
        "    (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 10 " +
        "      + f.view_count " +
        "      + (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 1000 " +
        "        / (f.view_count + 10) < :cursorScore " +
        "    OR (" +
        "      (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 10 " +
        "        + f.view_count " +
        "        + (COALESCE(r.reaction_cnt, 0) + COALESCE(c.comment_cnt, 0)) * 1000 " +
        "          / (f.view_count + 10) = :cursorScore " +
        "      AND f.feed_id < :cursorId" +
        "    )" +
        "  ) " +
        "ORDER BY popularity_score DESC, f.feed_id DESC",
        nativeQuery = true)
    List<Object[]> findPopularFeeds(
        @Param("since") LocalDateTime since,
        @Param("cursorScore") Long cursorScore,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );
}
