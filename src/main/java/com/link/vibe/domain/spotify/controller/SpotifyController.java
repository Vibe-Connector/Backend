package com.link.vibe.domain.spotify.controller;

import com.link.vibe.domain.spotify.dto.SpotifyTrackResponse;
import com.link.vibe.domain.spotify.service.SpotifyService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Spotify", description = "Spotify 트랙 검색 API — ISRC, MBID, 트랙명으로 검색")
@RestController
@RequestMapping("/api/v1/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;

    @Operation(
            summary = "Spotify 트랙 검색",
            description = """
                    ISRC, MBID(MusicBrainz ID), 또는 트랙명+아티스트로 Spotify에서 트랙을 검색합니다.

                    **우선순위:** isrc > mbid > query+artist

                    **응답:** 앨범 커버, 미리듣기 URL, Spotify 재생 링크 등
                    """
    )
    @GetMapping("/search")
    public ApiResponse<SpotifyTrackResponse> search(
            @Parameter(description = "ISRC (국제 표준 녹음 코드)") @RequestParam(required = false) String isrc,
            @Parameter(description = "MusicBrainz ID") @RequestParam(required = false) String mbid,
            @Parameter(description = "트랙명") @RequestParam(required = false) String query,
            @Parameter(description = "아티스트명") @RequestParam(required = false) String artist
    ) {
        SpotifyTrackResponse result;

        if (isrc != null && !isrc.isBlank()) {
            result = spotifyService.searchByIsrc(isrc)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND, "ISRC에 해당하는 트랙을 찾을 수 없습니다."));
        } else if (mbid != null && !mbid.isBlank()) {
            result = spotifyService.searchByMbid(mbid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND, "MBID에 해당하는 트랙을 찾을 수 없습니다."));
        } else if (query != null && !query.isBlank()) {
            result = spotifyService.searchByQuery(query, artist)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND, "검색 결과가 없습니다."));
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "isrc, mbid, query 중 하나를 입력해야 합니다.");
        }

        return ApiResponse.ok(result);
    }
}
