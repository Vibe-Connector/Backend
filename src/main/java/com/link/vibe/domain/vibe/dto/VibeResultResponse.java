package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Vibe 생성/상세 응답")
public record VibeResultResponse(
        @Schema(description = "세션 ID") Long sessionId,
        @Schema(description = "AI 생성 분위기 문구") String phrase,
        @Schema(description = "AI 분석 결과") String analysis,
        @Schema(description = "선택한 옵션 정보") SelectedOptions selectedOptions,
        @Schema(description = "AI 처리 시간 (ms)") Integer processingTimeMs,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
    @Schema(description = "선택한 옵션 상세")
    public record SelectedOptions(
            @Schema(description = "선택한 기분 키워드 값 목록") List<String> moods,
            @Schema(description = "선택한 시간 키") String time,
            @Schema(description = "선택한 날씨 키") String weather,
            @Schema(description = "선택한 공간 키") String place,
            @Schema(description = "선택한 동반자 키") String companion
    ) {}
}
