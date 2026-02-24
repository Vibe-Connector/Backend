package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Vibe 히스토리 항목")
public record VibeHistoryResponse(
        @Schema(description = "세션 ID") Long sessionId,
        @Schema(description = "AI 생성 분위기 문구") String phrase,
        @Schema(description = "선택한 기분 키워드 값 목록") List<String> moods,
        @Schema(description = "선택한 시간 키") String time,
        @Schema(description = "선택한 날씨 키") String weather,
        @Schema(description = "선택한 공간 키") String place,
        @Schema(description = "선택한 동반자 키") String companion,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {}
