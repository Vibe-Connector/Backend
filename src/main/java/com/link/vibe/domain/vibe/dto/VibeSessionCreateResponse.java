package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Vibe 세션 생성 응답")
public record VibeSessionCreateResponse(

        @Schema(description = "세션 ID", example = "1")
        Long sessionId,

        @Schema(description = "세션 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "세션 생성 시각")
        LocalDateTime createdAt
) {}
