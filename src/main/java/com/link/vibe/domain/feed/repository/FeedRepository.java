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

    boolean existsByVibeResultResultIdAndIsPublicTrue(Long resultId);

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

    // ── Strategy D: 비슷한 무드 추천 (PostgreSQL 옵션 매칭) ──

    @Query(value =
        "WITH src AS ( " +
        "    SELECT vp.mood_keyword_ids, vp.time_id, vp.weather_id, vp.place_id, vp.companion_id " +
        "    FROM feeds sf " +
        "    JOIN vibe_results svr ON svr.result_id = sf.result_id " +
        "    JOIN vibe_sessions svs ON svs.session_id = svr.session_id " +
        "    JOIN vibe_prompts vp ON vp.session_id = svs.session_id " +
        "    WHERE sf.feed_id = :feedId " +
        "), " +
        "scored AS ( " +
        "    SELECT f.feed_id, f.caption, f.created_at, " +
        "           u.user_id AS author_id, u.nickname AS author_nickname, u.profile_image_url, " +
        "           vr.result_id, vr.generated_image_url, vr.phrase, " +
        "           ( " +
        "             COALESCE( " +
        "               (SELECT COUNT(*) " +
        "                FROM jsonb_array_elements_text(COALESCE(tvp.mood_keyword_ids, '[]'::jsonb)) AS mk(val) " +
        "                WHERE mk.val IN ( " +
        "                  SELECT jsonb_array_elements_text(COALESCE(src.mood_keyword_ids, '[]'::jsonb)) " +
        "                )), 0 " +
        "             ) * 2 " +
        "             + CASE WHEN tvp.time_id = src.time_id AND tvp.time_id IS NOT NULL THEN 1 ELSE 0 END " +
        "             + CASE WHEN tvp.weather_id = src.weather_id AND tvp.weather_id IS NOT NULL THEN 1 ELSE 0 END " +
        "             + CASE WHEN tvp.place_id = src.place_id AND tvp.place_id IS NOT NULL THEN 1 ELSE 0 END " +
        "             + CASE WHEN tvp.companion_id = src.companion_id AND tvp.companion_id IS NOT NULL THEN 1 ELSE 0 END " +
        "           ) AS similarity_score " +
        "    FROM feeds f " +
        "    JOIN users u ON u.user_id = f.user_id " +
        "    JOIN vibe_results vr ON vr.result_id = f.result_id " +
        "    JOIN vibe_sessions vs ON vs.session_id = vr.session_id " +
        "    JOIN vibe_prompts tvp ON tvp.session_id = vs.session_id " +
        "    CROSS JOIN src " +
        "    WHERE f.is_public = true " +
        "      AND f.deleted_at IS NULL " +
        "      AND f.feed_id <> :feedId " +
        ") " +
        "SELECT * FROM scored " +
        "WHERE similarity_score > 0 " +
        "ORDER BY similarity_score DESC, feed_id DESC",
        nativeQuery = true)
    List<Object[]> findSimilarFeeds(@Param("feedId") Long feedId, Pageable pageable);

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
        "    SELECT feed_id, COUNT(*) AS comment_cnt " +
        "    FROM feed_comments " +
        "    WHERE deleted_at IS NULL " +
        "    GROUP BY feed_id" +
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
