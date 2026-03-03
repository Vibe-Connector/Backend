package com.link.vibe.domain.archive.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "즐겨찾기 토글 응답")
public record FavoriteResponse(

        @Schema(description = "즐겨찾기 상태 (true = 등록됨, false = 해제됨)", example = "true")
        boolean favorited
) {}
