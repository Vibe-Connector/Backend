package com.link.vibe.domain.option.dto;

import java.util.List;

public record OptionResponse(
        List<MoodDto> moods,
        List<TimeDto> times,
        List<WeatherDto> weathers,
        List<PlaceDto> places,
        List<CompanionDto> companions
) {
    public record MoodDto(Long keywordId, String keywordValue, String category) {}
    public record TimeDto(Long timeId, String timeKey, String timeValue, String period) {}
    public record WeatherDto(Long weatherId, String weatherKey) {}
    public record PlaceDto(Long placeId, String placeKey) {}
    public record CompanionDto(Long companionId, String companionKey) {}
}
