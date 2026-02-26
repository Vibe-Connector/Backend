package com.link.vibe.domain.user.dto;

import com.link.vibe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 프로필 응답")
public record UserProfileResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "닉네임", example = "바이브유저")
        String nickname,

        @Schema(description = "성별", example = "MALE")
        String gender,

        @Schema(description = "출생 연도", example = "1995")
        Integer birthYear,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "선호 언어 ID", example = "1")
        Long preferredLanguageId,

        @Schema(description = "마지막 로그인 시각")
        LocalDateTime lastLoginAt,

        @Schema(description = "가입일")
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getGender(),
                user.getBirthYear(),
                user.getProfileImageUrl(),
                user.getPreferredLanguageId(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
