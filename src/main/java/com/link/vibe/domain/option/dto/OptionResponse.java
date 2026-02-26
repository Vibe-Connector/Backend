package com.link.vibe.domain.option.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "전체 옵션 응답 (i18n 적용)")
public record OptionResponse(
        List<MoodDto> moods,
        List<TimeDto> times,
        List<WeatherDto> weathers,
        List<PlaceDto> places,
        List<CompanionDto> companions
) {
    @Schema(description = "기분 키워드")
    public record MoodDto(
            @Schema(description = "키워드 ID") Long keywordId,
            @Schema(description = "시스템 키", example = "cozy") String keywordValue,
            @Schema(description = "카테고리", example = "분위기") String category,
            @Schema(description = "번역된 표시 텍스트", example = "아늑한") String label
    ) {}

    @Schema(description = "시간대 옵션")
    public record TimeDto(
            @Schema(description = "시간대 ID") Long timeId,
            @Schema(description = "시스템 키", example = "morning") String timeKey,
            @Schema(description = "시간 값", example = "09:00") String timeValue,
            @Schema(description = "AM/PM", example = "AM") String period
    ) {}

    @Schema(description = "날씨 옵션")
    public record WeatherDto(
            @Schema(description = "날씨 ID") Long weatherId,
            @Schema(description = "시스템 키", example = "sunny") String weatherKey,
            @Schema(description = "번역된 표시 텍스트", example = "맑음") String label
    ) {}

    @Schema(description = "공간 옵션")
    public record PlaceDto(
            @Schema(description = "공간 ID") Long placeId,
            @Schema(description = "시스템 키", example = "home") String placeKey,
            @Schema(description = "번역된 표시 텍스트", example = "집") String label
    ) {}

    @Schema(description = "동반자 옵션")
    public record CompanionDto(
            @Schema(description = "동반자 ID") Long companionId,
            @Schema(description = "시스템 키", example = "alone") String companionKey,
            @Schema(description = "번역된 표시 텍스트", example = "혼자") String label
    ) {}
}
