package com.link.vibe.domain.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "영화 상세 응답")
public record MovieDetailResponse(
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

        // ── 영화 상세 ──
        @Schema(description = "TMDB ID") Integer tmdbId,
        @Schema(description = "원제") String originalTitle,
        @Schema(description = "줄거리") String overview,
        @Schema(description = "개봉일") LocalDate releaseDate,
        @Schema(description = "상영 시간 (분)") Integer runtime,
        @Schema(description = "평점") BigDecimal voteAverage,
        @Schema(description = "투표 수") Integer voteCount,
        @Schema(description = "포스터 경로") String posterPath,
        @Schema(description = "장르") List<String> genres,
        @Schema(description = "출연진") List<CastInfo> castInfo,
        @Schema(description = "콘텐츠 타입 (MOVIE/TV)") String contentType
) {
    public record CastInfo(String name, String role) {}
}
