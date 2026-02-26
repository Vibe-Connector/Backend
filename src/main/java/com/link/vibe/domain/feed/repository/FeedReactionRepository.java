package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.FeedReaction;
import com.link.vibe.domain.feed.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

    @Query("SELECT fr.reactionType, COUNT(fr) FROM FeedReaction fr " +
           "WHERE fr.feed.feedId = :feedId GROUP BY fr.reactionType")
    List<Object[]> countByFeedIdGroupByReactionType(@Param("feedId") Long feedId);

    Optional<FeedReaction> findByFeedFeedIdAndUserUserIdAndReactionType(
        Long feedId, Long userId, ReactionType reactionType);

    List<FeedReaction> findByFeedFeedIdAndUserUserId(Long feedId, Long userId);
}
