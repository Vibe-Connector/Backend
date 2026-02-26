package com.link.vibe.domain.item.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "음악 상세 응답")
public record MusicDetailResponse(
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

        // ── 음악 상세 ──
        @Schema(description = "아티스트") List<ArtistInfo> artists,
        @Schema(description = "앨범명") String albumName,
        @Schema(description = "앨범 커버 URL") String albumCoverUrl,
        @Schema(description = "트랙 길이 (ms)") Integer trackDurationMs,
        @Schema(description = "발매일") LocalDate releaseDate,
        @Schema(description = "장르") List<String> genres,
        @Schema(description = "미리듣기 URL") String previewUrl,
        @Schema(description = "Spotify URI") String spotifyUri,
        @Schema(description = "콘텐츠 타입 (TRACK/ALBUM/PLAYLIST)") String contentType
) {
    public record ArtistInfo(String name, String role) {}
}
