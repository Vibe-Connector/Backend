package com.link.vibe.domain.spotify.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Spotify 트랙 조회 결과")
public record SpotifyTrackResponse(
        @Schema(description = "Spotify 트랙 ID") String spotifyId,
        @Schema(description = "트랙 이름") String trackName,
        @Schema(description = "아티스트 목록") List<String> artists,
        @Schema(description = "앨범 이름") String albumName,
        @Schema(description = "앨범 커버 URL (640px)") String albumCoverUrl,
        @Schema(description = "앨범 커버 URL (300px)") String albumCoverMediumUrl,
        @Schema(description = "앨범 커버 URL (64px)") String albumCoverSmallUrl,
        @Schema(description = "30초 미리듣기 URL") String previewUrl,
        @Schema(description = "Spotify URI (spotify:track:xxx)") String spotifyUri,
        @Schema(description = "Spotify 외부 링크") String spotifyUrl,
        @Schema(description = "트랙 길이 (ms)") Integer durationMs,
        @Schema(description = "ISRC") String isrc
) {}
