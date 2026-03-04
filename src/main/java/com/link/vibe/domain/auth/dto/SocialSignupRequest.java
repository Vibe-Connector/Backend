package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "소셜 회원가입 요청")
public record SocialSignupRequest(

        @NotBlank
        @Schema(description = "소셜 회원가입 임시 토큰")
        String socialSignupToken,

        @NotBlank
        @Schema(description = "닉네임")
        String nickname,

        @NotBlank
        @Size(min = 8)
        @Schema(description = "비밀번호")
        String password
) {}
