package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
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

    @Query(value =
        "SELECT f.feed_id, f.caption, f.view_count, f.created_at, " +
        "       u.user_id AS author_id, u.nickname AS author_nickname, u.profile_image_url, " +
        "       vr.generated_image_url, " +
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
