package com.link.vibe.domain.vibe.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "날씨 강도 정보")
public record WeatherIntensityDto(
        @Schema(description = "날씨 옵션 ID", example = "1")
        @NotNull Long weatherId,

        @Schema(description = "강도 (0~100)", example = "70")
        @NotNull @Min(0) @Max(100) Integer intensity
) {}
