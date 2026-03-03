package com.link.vibe.domain.notification.dto;

import com.link.vibe.domain.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(

        @Schema(description = "알림 ID", example = "1") Long notificationId,

        @Schema(description = "알림 유형", example = "FOLLOW") String type,

        @Schema(description = "알림 제목", example = "새 팔로워") String title,

        @Schema(description = "알림 본문", example = "user1님이 회원님을 팔로우했습니다") String body,

        @Schema(description = "이동 URL", example = "/users/2") String linkUrl,

        @Schema(description = "참조 ID", example = "2") Long referenceId,

        @Schema(description = "읽음 여부", example = "false") boolean isRead,

        @Schema(description = "생성 시각") LocalDateTime createdAt,

        @Schema(description = "읽은 시각") LocalDateTime readAt) {
    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLinkUrl(),
                notification.getReferenceId(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
