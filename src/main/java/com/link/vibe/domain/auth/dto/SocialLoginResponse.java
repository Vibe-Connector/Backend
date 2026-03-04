package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 로그인 응답")
public record SocialLoginResponse(

        @Schema(description = "사용자 ID (신규 유저 시 null)", example = "1")
        Long userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "닉네임 (신규 유저 시 null)", example = "바이브유저")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "Access Token (신규 유저 시 null)")
        String accessToken,

        @Schema(description = "Refresh Token (신규 유저 시 null)")
        String refreshToken,

        @Schema(description = "신규 가입 필요 여부", example = "true")
        boolean isNewUser,

        @Schema(description = "소셜 회원가입용 임시 토큰 (신규 유저만)")
        String socialSignupToken
) {}
