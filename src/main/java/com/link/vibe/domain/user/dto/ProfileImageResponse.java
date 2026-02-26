package com.link.vibe.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 이미지 업로드 응답")
public record ProfileImageResponse(

        @Schema(description = "업로드된 프로필 이미지 URL", example = "https://vibelink-uploads.s3.ap-northeast-2.amazonaws.com/profiles/xxx.jpg")
        String profileImageUrl
) {}
