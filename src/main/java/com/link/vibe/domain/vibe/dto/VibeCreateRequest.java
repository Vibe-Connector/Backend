package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Vibe 생성 요청")
public record VibeCreateRequest(
        @Schema(description = "선택한 기분 키워드 ID 목록", example = "[1, 3, 7]")
        @NotEmpty List<Long> moodKeywordIds,

        @Schema(description = "시간 옵션 ID", example = "1")
        @NotNull Long timeId,

        @Schema(description = "날씨 옵션 ID", example = "1")
        @NotNull Long weatherId,

        @Schema(description = "공간 옵션 ID", example = "1")
        @NotNull Long placeId,

        @Schema(description = "동반자 옵션 ID", example = "1")
        @NotNull Long companionId
) {}
