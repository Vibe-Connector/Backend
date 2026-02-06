package com.link.vibe.domain.option.dto;

import java.util.List;

public record OptionResponse(
        List<MoodDto> moods,
        List<TimeDto> times,
        List<WeatherDto> weathers,
        List<PlaceDto> places,
        List<CompanionDto> companions
) {
    public record MoodDto(Long keywordId, String keywordKey, String keywordText, String category) {}
    public record TimeDto(Long timeId, String timeKey, String timeText) {}
    public record WeatherDto(Long weatherId, String weatherKey, String weatherText, String icon) {}
    public record PlaceDto(Long placeId, String placeKey, String placeText, String icon) {}
    public record CompanionDto(Long companionId, String companionKey, String companionText, String icon) {}
}
