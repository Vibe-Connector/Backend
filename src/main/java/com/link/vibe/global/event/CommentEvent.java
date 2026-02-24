package com.link.vibe.global.event;

public record CommentEvent(Long feedId, Long commentId, Long actorUserId, Long parentCommentId) {
}
