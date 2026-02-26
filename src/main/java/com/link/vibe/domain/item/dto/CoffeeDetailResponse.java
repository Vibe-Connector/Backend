package com.link.vibe.domain.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "커피 캡슐 상세 응답")
public record CoffeeDetailResponse(
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

        // ── 커피 상세 ──
        @Schema(description = "캡슐명") String capsuleName,
        @Schema(description = "라인 (ORIGINAL/VERTUO)") String line,
        @Schema(description = "서브 카테고리") String subCategory,
        @Schema(description = "강도") Integer intensity,
        @Schema(description = "최대 강도") Integer intensityMax,
        @Schema(description = "컵 사이즈") List<CupSize> cupSizes,
        @Schema(description = "원두 종류") String beanType,
        @Schema(description = "원산지") List<String> origins,
        @Schema(description = "로스팅 레벨") String roastLevel,
        @Schema(description = "아로마 프로필") AromaProfile aromaProfile,
        @Schema(description = "맛 노트") String flavorNotes,
        @Schema(description = "바디감 (1~5)") Integer body,
        @Schema(description = "쓴맛 (1~5)") Integer bitterness,
        @Schema(description = "산미 (1~5)") Integer acidity,
        @Schema(description = "로스팅 (1~5)") Integer roasting,
        @Schema(description = "디카페인 여부") Boolean isDecaf,
        @Schema(description = "한정판 여부") Boolean isLimitedEdition,
        @Schema(description = "캡슐당 가격 (원)") Integer pricePerCapsuleKrw
) {
    public record CupSize(String type, Integer ml) {}
    public record AromaProfile(List<String> primary) {}
}
