package com.link.vibe.domain.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팔로우 응답")
public record FollowResponse(

        @Schema(description = "팔로우 상태 (true = 팔로우 중)", example = "true")
        boolean following,

        @Schema(description = "해당 사용자의 팔로워 수", example = "42")
        long followerCount
) {}
