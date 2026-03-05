package com.link.vibe.domain.vibe.dto.aiserver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AIServerRecommendResponse(
        boolean success,
        AIServerRecommendData data,
        String code,
        String message
) {

    public record AIServerRecommendData(
            String phrase,
            String analysis,
            List<AIServerCategoryRec> recommendations,
            @JsonProperty("processing_time_ms") int processingTimeMs,
            @JsonProperty("graph_context") Map<String, Object> graphContext
    ) {}

    public record AIServerCategoryRec(
            @JsonProperty("category_key") String categoryKey,
            List<AIServerItem> items
    ) {}

    public record AIServerItem(
            @JsonProperty("item_id") int itemId,
            @JsonProperty("item_key") String itemKey,
            String name,
            String category,
            @JsonProperty("relevance_score") double relevanceScore,
            String reason,
            @JsonProperty("image_url") String imageUrl,
            String brand,
            @JsonProperty("external_link") String externalLink,
            @JsonProperty("external_service") String externalService
    ) {}
}
