package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 코드 검증 요청")
public record VerifyCodeRequest(

        @Schema(description = "인증 코드를 받은 이메일 주소", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Schema(description = "6자리 인증 코드", example = "123456")
        @NotBlank(message = "인증 코드는 필수입니다")
        String code
) {}
