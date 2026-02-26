package com.link.vibe.domain.user.dto;

import com.link.vibe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공개 사용자 프로필 응답")
public record PublicUserProfileResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "바이브유저")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl
) {
    public static PublicUserProfileResponse from(User user) {
        return new PublicUserProfileResponse(
                user.getUserId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
