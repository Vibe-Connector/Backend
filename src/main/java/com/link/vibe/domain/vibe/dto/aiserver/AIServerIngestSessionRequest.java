package com.link.vibe.domain.vibe.dto.aiserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AIServerIngestSessionRequest(
        @JsonProperty("session_id") long sessionId,
        @JsonProperty("user_id") long userId,
        @JsonProperty("mood_keywords") List<String> moodKeywords,
        @JsonProperty("mood_keyword_ids") List<Integer> moodKeywordIds,
        @JsonProperty("time_key") String timeKey,
        @JsonProperty("time_id") Integer timeId,
        @JsonProperty("weather_key") String weatherKey,
        @JsonProperty("weather_id") Integer weatherId,
        @JsonProperty("place_key") String placeKey,
        @JsonProperty("place_id") Integer placeId,
        @JsonProperty("companion_key") String companionKey,
        @JsonProperty("companion_id") Integer companionId,
        String phrase,
        String analysis,
        @JsonProperty("recommended_item_ids") List<Integer> recommendedItemIds
) {}
