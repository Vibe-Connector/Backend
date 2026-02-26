package com.link.vibe.domain.user.dto;

import com.link.vibe.domain.user.entity.UserSettings;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 설정 응답")
public record UserSettingsResponse(

        @Schema(description = "푸시 알림 수신 여부", example = "true")
        Boolean pushEnabled,

        @Schema(description = "이메일 알림 수신 여부", example = "true")
        Boolean emailNotification,

        @Schema(description = "기본 공유 범위 (PUBLIC / PRIVATE)", example = "PRIVATE")
        String defaultSharePrivacy
) {
    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getPushEnabled(),
                settings.getEmailNotification(),
                settings.getDefaultSharePrivacy()
        );
    }
}
