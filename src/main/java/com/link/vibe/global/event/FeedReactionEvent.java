package com.link.vibe.global.event;

public record FeedReactionEvent(Long feedId, Long actorUserId, String reactionType) {
}
