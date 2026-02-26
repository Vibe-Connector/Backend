package com.link.vibe.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "사용자 설정 수정 요청")
public record UpdateSettingsRequest(

        @Schema(description = "푸시 알림 수신 여부", example = "true")
        Boolean pushEnabled,

        @Schema(description = "이메일 알림 수신 여부", example = "false")
        Boolean emailNotification,

        @Schema(description = "기본 공유 범위 (PUBLIC / PRIVATE)", example = "PUBLIC")
        @Pattern(regexp = "^(PUBLIC|PRIVATE)$", message = "공유 범위는 PUBLIC 또는 PRIVATE만 가능합니다.")
        String defaultSharePrivacy
) {}
