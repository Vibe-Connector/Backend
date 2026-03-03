package com.link.vibe.domain.user.dto;

import com.link.vibe.domain.user.entity.SocialAccount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "소셜 계정 연동 정보")
public record SocialAccountResponse(
        @Schema(description = "소셜 계정 ID") Long socialId,
        @Schema(description = "소셜 제공자 (GOOGLE, NAVER)") String provider,
        @Schema(description = "연동 시각") LocalDateTime linkedAt
) {
    public static SocialAccountResponse from(SocialAccount sa) {
        return new SocialAccountResponse(sa.getSocialId(), sa.getProvider(), sa.getCreatedAt());
    }
}
