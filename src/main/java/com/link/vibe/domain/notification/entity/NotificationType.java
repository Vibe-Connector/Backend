package com.link.vibe.domain.notification.entity;

/**
 * 알림 유형 — DB notifications.type ENUM과 1:1 대응
 */
public enum NotificationType {
    FEED_REACTION,
    FEED_COMMENT,
    FOLLOW,
    VIBE_COMPLETE,
    REPORT_READY,
    SYSTEM
}
