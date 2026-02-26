package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record SocialLoginRequest(

        @Schema(description = "소셜 로그인 인가 코드", example = "4/0AfJohXn...")
        @NotBlank(message = "인가 코드는 필수입니다")
        String authorizationCode,

        @Schema(description = "OAuth 리다이렉트 URI", example = "http://localhost:3000/auth/callback")
        @NotBlank(message = "리다이렉트 URI는 필수입니다")
        String redirectUri
) {}
