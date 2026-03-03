package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Vibe 프롬프트 제출 응답")
public record VibePromptSubmitResponse(
        @Schema(description = "세션 ID") Long sessionId,
        @Schema(description = "AI 생성 분위기 문구") String phrase,
        @Schema(description = "AI 분석 결과") String analysis,
        @Schema(description = "선택한 옵션 정보") VibeResultResponse.SelectedOptions selectedOptions,
        @Schema(description = "카테고리별 추천 아이템") List<CategoryRecommendation> recommendations,
        @Schema(description = "AI 처리 시간 (ms)") Integer processingTimeMs,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {}
