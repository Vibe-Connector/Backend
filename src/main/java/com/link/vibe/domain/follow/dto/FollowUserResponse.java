package com.link.vibe.domain.follow.dto;

import com.link.vibe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팔로우 사용자 정보")
public record FollowUserResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "vibe_user")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "현재 로그인 사용자의 팔로우 여부", example = "true")
        boolean following
) {
    public static FollowUserResponse of(User user, boolean following) {
        return new FollowUserResponse(
                user.getUserId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                following
        );
    }
}
