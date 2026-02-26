package com.link.vibe.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 중복 확인 응답")
public record CheckNicknameResponse(

        @Schema(description = "사용 가능 여부 (true = 사용 가능)", example = "true")
        boolean available
) {}
