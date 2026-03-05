package com.link.vibe.domain.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "반응 사용자 정보")
public record ReactionUserResponse(
    @Schema(description = "사용자 ID") Long userId,
    @Schema(description = "닉네임") String nickname,
    @Schema(description = "프로필 이미지 URL") String profileImageUrl,
    @Schema(description = "반응 유형") String reactionType
) {}
