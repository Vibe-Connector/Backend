package com.link.vibe.domain.feed.repository;

import com.link.vibe.domain.feed.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByCommentCommentIdAndUserUserId(Long commentId, Long userId);

    long countByCommentCommentId(Long commentId);

    boolean existsByCommentCommentIdAndUserUserId(Long commentId, Long userId);
}
