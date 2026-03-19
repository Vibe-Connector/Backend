package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "추천 아이템 정보")
public record RecommendedItemResponse(
        @Schema(description = "아이템 ID") Long itemId,
        @Schema(description = "아이템 키") String itemKey,
        @Schema(description = "아이템 이름 (번역)") String itemName,
        @Schema(description = "카테고리 키") String categoryKey,
        @Schema(description = "브랜드") String brand,
        @Schema(description = "이미지 URL") String imageUrl,
        @Schema(description = "외부 링크") String externalLink,
        @Schema(description = "외부 서비스") String externalService,
        @Schema(description = "매칭 점수 (0~1)") BigDecimal matchScore,
        @Schema(description = "추천 이유") String recommendReason,

        // ── 음악 전용 필드 (music 카테고리일 때만 값이 있음) ──
        @Schema(description = "앨범 커버 URL (음악만)") String albumCoverUrl,
        @Schema(description = "미리듣기 URL (음악만)") String previewUrl,
        @Schema(description = "Spotify URI (음악만)") String spotifyUri,
        @Schema(description = "ISRC (음악만)") String isrc,
        @Schema(description = "MusicBrainz ID (음악만)") String musicbrainzId
) {}
