package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카테고리별 추천 아이템 그룹")
public record CategoryRecommendation(
        @Schema(description = "카테고리 키 (movie, music, lighting, coffee)") String categoryKey,
        @Schema(description = "추천 아이템 목록") List<RecommendedItemResponse> items
) {}
