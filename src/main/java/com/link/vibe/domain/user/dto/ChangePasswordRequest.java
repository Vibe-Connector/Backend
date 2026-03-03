package com.link.vibe.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호")
        @NotBlank(message = "현재 비밀번호는 필수입니다")
        String currentPassword,

        @Schema(description = "새 비밀번호 (8자 이상)")
        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
        String newPassword
) {}
