package com.link.vibe.domain.vibe.dto.aiserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AIServerRecommendRequest(
        @JsonProperty("mood_keyword_ids") List<Integer> moodKeywordIds,
        @JsonProperty("mood_keywords") List<String> moodKeywords,
        @JsonProperty("time_id") int timeId,
        @JsonProperty("time_key") String timeKey,
        @JsonProperty("weather_id") int weatherId,
        @JsonProperty("weather_key") String weatherKey,
        @JsonProperty("place_id") int placeId,
        @JsonProperty("place_key") String placeKey,
        @JsonProperty("companion_id") int companionId,
        @JsonProperty("companion_key") String companionKey,
        Integer hour,
        Integer minute,
        @JsonProperty("weather_intensities") List<AIServerWeatherIntensity> weatherIntensities,
        @JsonProperty("max_items_per_category") int maxItemsPerCategory
) {

    public record AIServerWeatherIntensity(
            @JsonProperty("weather_option_id") int weatherOptionId,
            double intensity
    ) {}
}
