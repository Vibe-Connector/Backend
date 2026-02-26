package com.link.vibe.domain.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "조명 상세 응답")
public record LightingDetailResponse(
        // ── 공통 정보 ──
        @Schema(description = "아이템 ID") Long itemId,
        @Schema(description = "아이템 이름 (번역)") String itemName,
        @Schema(description = "아이템 설명 (번역)") String description,
        @Schema(description = "카테고리 키") String categoryKey,
        @Schema(description = "카테고리 이름 (번역)") String categoryName,
        @Schema(description = "브랜드") String brand,
        @Schema(description = "이미지 URL") String imageUrl,
        @Schema(description = "외부 링크") String externalLink,
        @Schema(description = "외부 서비스") String externalService,

        // ── 조명 상세 ──
        @Schema(description = "색온도 (K)") Integer colorTempKelvin,
        @Schema(description = "색온도 이름") String colorTempName,
        @Schema(description = "밝기 (%)") Integer brightnessPercent,
        @Schema(description = "밝기 레벨") String brightnessLevel,
        @Schema(description = "조명 유형") String lightingType,
        @Schema(description = "조명 색상") String lightColor,
        @Schema(description = "설치 위치") String position,
        @Schema(description = "공간 맥락") String spaceContext,
        @Schema(description = "시간 맥락") String timeContext,
        @Schema(description = "다이나믹 여부") Boolean isDynamic
) {}
