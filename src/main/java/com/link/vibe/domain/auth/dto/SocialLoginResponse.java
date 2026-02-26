package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 로그인 응답")
public record SocialLoginResponse(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임", example = "바이브유저")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "Access Token (Bearer)")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken,

        @Schema(description = "신규 가입 여부 (true면 프론트에서 추가 정보 입력 유도)", example = "true")
        boolean isNewUser
) {}
