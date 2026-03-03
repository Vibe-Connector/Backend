package com.link.vibe.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 알림 수 응답")
public record UnreadCountResponse(

        @Schema(description = "읽지 않은 알림 수", example = "5") long unreadCount) {
}
