package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이템 좋아요 토글 응답")
public record VibeItemLikeResponse(
        @Schema(description = "Vibe 아이템 ID") Long vibeItemId,
        @Schema(description = "좋아요 여부") Boolean isLiked
) {}
