package com.link.vibe.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 수정 요청")
public record UpdateProfileRequest(

        @Schema(description = "닉네임 (2~20자)", example = "새닉네임")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
        String nickname,

        @Schema(description = "이름", example = "홍길동")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        @Schema(description = "성별 (MALE, FEMALE, OTHER)", example = "MALE")
        String gender,

        @Schema(description = "출생 연도", example = "1995")
        Integer birthYear,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/new-profile.jpg")
        @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
        String profileImageUrl,

        @Schema(description = "선호 언어 ID", example = "1")
        Long preferredLanguageId
) {}
