package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Vibe 생성 요청")
public record VibeCreateRequest(
        @Schema(description = "선택한 기분 키워드 ID 목록", example = "[1, 3, 7]")
        @NotEmpty List<Long> moodKeywordIds,

        @Schema(description = "시간 옵션 ID", example = "1")
        @NotNull Long timeId,

        @Schema(description = "날씨 옵션 ID (하위 호환용, weatherIntensities가 있으면 가장 높은 intensity의 ID로 자동 설정)", example = "1")
        @NotNull Long weatherId,

        @Schema(description = "공간 옵션 ID", example = "1")
        @NotNull Long placeId,

        @Schema(description = "동반자 옵션 ID", example = "1")
        @NotNull Long companionId,

        @Schema(description = "선택한 시(hour), 0~23", example = "15")
        @Min(0) @Max(23) Integer hour,

        @Schema(description = "선택한 분(minute), 0~59", example = "25")
        @Min(0) @Max(59) Integer minute,

        @Schema(description = "날씨 강도 목록 (복수 날씨 조합)")
        @Valid List<WeatherIntensityDto> weatherIntensities
) {}
